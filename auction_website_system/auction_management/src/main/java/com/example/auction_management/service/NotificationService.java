package com.example.auction_management.service;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Bid;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Notification;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.repository.BidRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.NotificationRepository;
import com.example.auction_management.model.*;
import com.example.auction_management.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

// Import EmailService
import com.example.auction_management.service.EmailService;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService; // Inject EmailService
    private final FollowRepository followRepository;

    /**
     * Gửi thông báo cho userId với nội dung message.
     */
    public void sendNotification(Integer userId, String message, Auction auction) {
        logger.info("sendNotification() - userId: {}, message: {}", userId, message);
        Customer customer = customerRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Customer with ID {} not found!", userId);
                    return new RuntimeException("Customer not found");
                });

        logger.debug("Found customer with username: {}", customer.getAccount().getUsername());

        // Tạo Notification
        Notification notification = new Notification();
        notification.setCustomer(customer);
        notification.setMessage(message);
        notification.setTimestamp(LocalDateTime.now());
        notification.setAuction(auction);
        notification.setIsRead(false);

        // Lưu notification vào DB
        notificationRepository.save(notification);
        logger.debug("Notification saved to DB with ID: {}", notification.getNotification_id());

        // Gửi notification qua WebSocket
        String username = customer.getAccount().getUsername();
        logger.debug("Sending notification to username: {} at /queue/notifications", username);
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
        logger.info("Notification sent via WebSocket to user: {}", username);
    }

    /**
     * Gửi thông báo đặt giá (bid) cho tất cả người tham gia và người bán.
     * Phương thức mới nhận vào 2 thông điệp riêng: sellerMessage và participantMessage.
     */
    public void sendBidNotification(Integer auctionId, Integer bidderId, String sellerMessage, String participantMessage) {
        logger.info("sendBidNotification() - auctionId: {}, bidderId: {}", auctionId, bidderId);
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    logger.error("Auction with ID {} not found!", auctionId);
                    return new RuntimeException("Auction not found");
                });
        logger.debug("Found auction with ID: {}", auctionId);

        // Xây dựng thông tin phiên đấu giá
        String auctionInfo = String.format("Phiên đấu giá #%d - Sản phẩm: %s",
                auction.getAuctionId(),
                auction.getProduct().getName());

        // Lấy người bán (seller)
        Customer seller = getAuctionSeller(auction);
        logger.debug("Seller for auction ID {} is customer ID {}, username: {}",
                auctionId, seller.getCustomerId(), seller.getAccount().getUsername());

        // Lấy tất cả người tham gia
        List<Customer> participants = bidRepository.findDistinctCustomersByAuctionId(auctionId);
        logger.debug("Found {} participants for auction ID {}", participants.size(), auctionId);

        // Gửi thông báo cho những người tham gia (trừ bidder hiện tại)
        String participantFullMessage = auctionInfo + " - " + participantMessage;
        for (Customer participant : participants) {
            if (!participant.getCustomerId().equals(bidderId)) {
                sendNotification(participant.getCustomerId(), participantFullMessage, auction);
            }
        }

        // Gửi thông báo cho người bán (nếu người bán không phải chính bidder)
        if (!seller.getCustomerId().equals(bidderId)) {
            String sellerFullMessage = auctionInfo + " - " + sellerMessage;
            sendNotification(seller.getCustomerId(), sellerFullMessage, auction);
        }
    }

    /**
     * Lấy người bán của một phiên đấu giá.
     */
    private Customer getAuctionSeller(Auction auction) {
        return auction.getProduct().getAccount().getCustomer();
    }

    /**
     * Lấy danh sách thông báo của một customer theo thứ tự thời gian giảm dần.
     */
    public List<Notification> getNotificationsByCustomerId(Integer customerId) {
        try {
            logger.info("Fetching notifications for customerId: {}", customerId);
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> {
                        logger.error("Customer not found with ID: {}", customerId);
                        return new RuntimeException("Customer not found");
                    });
            List<Notification> notifications = notificationRepository.findByCustomer_CustomerIdOrderByTimestampDesc(customerId);
            logger.debug("Found {} notifications", notifications.size());
            return notifications;
        } catch (Exception e) {
            logger.error("Error fetching notifications for customerId {}: {}", customerId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch notifications");
        }
    }

    @Transactional
    public void markAsRead(Integer customerId, Integer auctionId) {
        notificationRepository.updateIsReadByCustomerAndAuction(customerId, auctionId);
    }

    /**
     * Gửi thông báo tổng hợp cho người dùng.
     * Thông báo này sẽ gộp chung các thông tin thông báo khác và ghi rõ phiên đấu giá (mã phiên và tên sản phẩm).
     */
    public void sendAggregatedNotification(Integer customerId, Auction auction, List<String> messages) {
        // Tạo thông tin phiên đấu giá rõ ràng
        String auctionInfo = String.format("Phiên đấu giá #%d - Sản phẩm: %s", auction.getAuctionId(), auction.getProduct().getName());

        // Gộp các thông báo riêng thành 1 thông báo, ngăn cách bởi dấu chấm phẩy
        String aggregatedMessage = auctionInfo + " - " + String.join("; ", messages);

        // Gửi thông báo
        sendNotification(customerId, aggregatedMessage, auction);
    }

    /**
     * Gửi thông báo & email khi phiên đấu giá kết thúc:
     * - Người bán: thông báo phiên đấu giá kết thúc.
     * - Người thắng: thông báo chúc mừng và yêu cầu thanh toán số tiền còn lại.
     * - Người không thắng: thông báo cảm ơn đã tham gia.
     */
    public void sendAuctionEndCommunications(Auction auction) {
        Integer auctionId = auction.getAuctionId();
        String productName = auction.getProduct().getName();
        String auctionInfo = String.format("Phiên đấu giá #%d - Sản phẩm: %s", auctionId, productName);

        // 1. Lấy người bán
        Customer seller = getAuctionSeller(auction);
        logger.debug("Seller for auction ID {}: customerId {} - username: {}",
                auctionId, seller.getCustomerId(), seller.getAccount().getUsername());

        // 2. Xác định bid thắng
        Optional<Bid> winningBidOptional = bidRepository.findTopByAuctionOrderByBidAmountDesc(auction);
        if (!winningBidOptional.isPresent()) {
            logger.warn("Không có bid nào cho phiên đấu giá: {}", auctionId);
            return;
        }
        Bid winningBid = winningBidOptional.get();
        Customer winner = winningBid.getCustomer();

        // 3. Lấy danh sách tất cả người tham gia
        List<Customer> participants = bidRepository.findDistinctCustomersByAuctionId(auctionId);
        logger.debug("Found {} participants for auction ID {}", participants.size(), auctionId);

        // 4. Tính số tiền cần thanh toán
        BigDecimal depositAmount = auction.getCurrentPrice().multiply(BigDecimal.valueOf(0.1));
        BigDecimal amountToPay = winningBid.getBidAmount().subtract(depositAmount);

        // 5. Gửi thông báo và email
        // Người bán
        String sellerMessage = auctionInfo + " - Phiên đấu giá của sản phẩm của bạn đã kết thúc.";
        sendNotification(seller.getCustomerId(), sellerMessage, auction);
        emailService.sendEmail(seller.getEmail(),
                "Thông báo kết thúc phiên đấu giá",
                generateSellerEmailContent(seller.getName(), productName));

        // Người tham gia
        for (Customer participant : participants) {
            if (participant.getCustomerId().equals(seller.getCustomerId())) {
                continue;
            }

            if (participant.getCustomerId().equals(winner.getCustomerId())) {
                // Người thắng
                String winnerMessage = String.format("%s - Chúc mừng, bạn đã thắng đấu giá! Vui lòng thanh toán số tiền còn lại: %s VNĐ.",
                        auctionInfo, amountToPay.toPlainString());
                sendNotification(participant.getCustomerId(), winnerMessage, auction);
                emailService.sendEmail(participant.getEmail(),
                        "Chúc mừng! Bạn đã thắng đấu giá",
                        generateWinnerEmailContent(participant.getName(), productName, amountToPay.toPlainString()));
            } else {
                // Người không thắng
                String loserMessage = auctionInfo + " - Phiên đấu giá đã kết thúc. Sản phẩm đã thuộc về người khác. Cảm ơn bạn đã tham gia đấu giá.";
                sendNotification(participant.getCustomerId(), loserMessage, auction);
                emailService.sendEmail(participant.getEmail(),
                        "Thông báo kết thúc phiên đấu giá",
                        generateLoserEmailContent(participant.getName(), productName));
            }
        }
    }

    private String generateSellerEmailContent(String name, String productName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.8; color: #333; margin: 0; padding: 0; font-size: 16px; }" +
                "        .content { padding: 25px; background-color: #f9f9f9; border-radius: 8px; max-width: 600px; margin: 20px auto; text-align: left; font-size: 18px; }" +
                "        .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 16px; color: #666; text-align: left; }" +
                "        .footer strong { color: #333; font-size: 17px; }" +
                "        .footer a { color: #007BFF; text-decoration: none; font-size: 16px; }" +
                "        .footer a:hover { text-decoration: underline; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "<div class='content'>" +
                "    <p style='font-size: 20px;'><strong>Xin chào, " + name + "!</strong></p>" +
                "    <p>Phiên đấu giá của sản phẩm <strong>\"" + productName + "\"</strong> đã kết thúc.</p>" +
                "    <p>Vui lòng kiểm tra và liên hệ với người tham gia nếu cần.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "    <p>Trân trọng,</p>" +
                "    <p><strong>C9-Stock</strong></p>" +
                "    <p>📍 Địa chỉ: 295 Nguyễn Tất Thành, Thanh Bình, Hải Châu, Đà Nẵng</p>" +
                "    <p>📞 Số điện thoại: <a href='tel:+84356789999'>+84 356789999</a></p>" +
                "    <p>✉ Email: <a href='mailto:daugiavn123@gmail.com'>daugiavn123@gmail.com</a></p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String generateWinnerEmailContent(String name, String productName, String amountToPay) {
        DecimalFormat formatter = new DecimalFormat("#,### VNĐ");
        String formattedAmount = formatter.format(amountToPay);

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.8; color: #333; margin: 0; padding: 0; font-size: 16px; }" +
                "        .content { padding: 25px; background-color: #f9f9f9; border-radius: 8px; max-width: 600px; margin: 20px auto; text-align: left; font-size: 18px; }" +
                "        .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 16px; color: #666; text-align: left; }" +
                "        .highlight { color: #28a745; font-weight: bold; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "<div class='content'>" +
                "    <p style='font-size: 20px;'><strong>Xin chào, " + name + "!</strong></p>" +
                "    <p class='highlight'>Chúc mừng bạn đã thắng đấu giá cho sản phẩm <strong>\"" + productName + "\"</strong>!</p>" +
                "    <p>Số tiền cần thanh toán là: <strong>" + formattedAmount + " VNĐ</strong>.</p>" +
                "    <p>Vui lòng thanh toán trong thời gian quy định để hoàn tất giao dịch.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "    <p>Trân trọng,</p>" +
                "    <p><strong>C9-Stock</strong></p>" +
                "    <p>📍 Địa chỉ: 295 Nguyễn Tất Thành, Thanh Bình, Hải Châu, Đà Nẵng</p>" +
                "    <p>📞 Số điện thoại: <a href='tel:+84356789999'>+84 356789999</a></p>" +
                "    <p>✉ Email: <a href='mailto:daugiavn123@gmail.com'>daugiavn123@gmail.com</a></p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String generateLoserEmailContent(String name, String productName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.8; color: #333; margin: 0; padding: 0; font-size: 16px; }" +
                "        .content { padding: 25px; background-color: #f9f9f9; border-radius: 8px; max-width: 600px; margin: 20px auto; text-align: left; font-size: 18px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "<div class='content'>" +
                "    <p style='font-size: 20px;'><strong>Xin chào, " + name + "!</strong></p>" +
                "    <p>Rất tiếc, bạn không chiến thắng phiên đấu giá cho sản phẩm <strong>\"" + productName + "\"</strong>.</p>" +
                "    <p>Tiền đặt cọc sẽ được hoàn trả trong thời gian sớm nhất.</p>" +
                "    <p>Cảm ơn bạn đã tham gia và hy vọng sẽ gặp lại bạn trong các phiên đấu giá tiếp theo.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "    <p>Trân trọng,</p>" +
                "    <p><strong>C9-Stock</strong></p>" +
                "    <p>📍 Địa chỉ: 295 Nguyễn Tất Thành, Thanh Bình, Hải Châu, Đà Nẵng</p>" +
                "    <p>📞 Số điện thoại: <a href='tel:+84356789999'>+84 356789999</a></p>" +
                "    <p>✉ Email: <a href='mailto:daugiavn123@gmail.com'>daugiavn123@gmail.com</a></p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
