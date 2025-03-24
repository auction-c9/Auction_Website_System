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
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Chúng ta cần import EmailService
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
     */
    public void sendBidNotification(Integer auctionId, Integer bidderId, String message) {
        logger.info("sendBidNotification() - auctionId: {}, bidderId: {}, message: {}", auctionId, bidderId, message);
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    logger.error("Auction with ID {} not found!", auctionId);
                    return new RuntimeException("Auction not found");
                });
        logger.debug("Found auction with ID: {}", auctionId);

        Customer bidder = customerRepository.findById(bidderId)
                .orElseThrow(() -> {
                    logger.error("Bidder with ID {} not found!", bidderId);
                    return new RuntimeException("Bidder not found");
                });
        logger.debug("Found bidder with username: {}", bidder.getAccount().getUsername());

        // Lấy người bán (seller)
        Customer seller = getAuctionSeller(auction);
        logger.debug("Seller for auction ID {} is customer ID {}, username: {}",
                auctionId, seller.getCustomerId(), seller.getAccount().getUsername());

        // Lấy tất cả người tham gia
        List<Customer> participants = bidRepository.findDistinctCustomersByAuctionId(auctionId);
        logger.debug("Found {} participants for auction ID {}", participants.size(), auctionId);

        String bidMessage = String.format("%s - %s", auctionInfo, message);
        for (Customer customer : participants) {
            if (!customer.getCustomerId().equals(bidderId)) {
                sendNotification(seller.getCustomerId(), bidMessage, auction);
            }
        }

        if (!seller.getCustomerId().equals(bidderId)) {
            String sellerMessage = String.format("%s - Có người vừa đặt giá thành công cho sản phẩm của bạn!", auctionInfo);
            sendNotification(seller.getCustomerId(), sellerMessage, auction);
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
        logger.info("getNotificationsByCustomerId() - customerId: {}", customerId);
        List<Notification> notifications = notificationRepository.findByCustomerCustomerIdOrderByTimestampDesc(customerId);
        logger.debug("Found {} notifications for customerId {}", notifications.size(), customerId);
        return notifications;
    }

    public void markAsRead(Integer customerId, Integer auctionId) {
        List<Notification> notifications = notificationRepository.findByCustomer_CustomerIdAndAuction_AuctionId(customerId, auctionId);
        notifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(notifications);
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

        // 3. Lấy danh sách tất cả người tham gia (không trùng lặp)
        List<Customer> participants = bidRepository.findDistinctCustomersByAuctionId(auctionId);
        logger.debug("Found {} participants for auction ID {}", participants.size(), auctionId);

        // 4. Tính số tiền cần thanh toán cho người thắng (tiền cọc = 10% currentPrice)
        BigDecimal depositAmount = auction.getCurrentPrice().multiply(BigDecimal.valueOf(0.1));
        BigDecimal amountToPay = winningBid.getBidAmount().subtract(depositAmount);

        // 5. Soạn nội dung thông báo và email cho từng đối tượng

        // Người bán
        String sellerMessage = auctionInfo + " - Phiên đấu giá của sản phẩm của bạn đã kết thúc.";
        String sellerEmailText = "Kính chào,\n\nPhiên đấu giá của sản phẩm \"" + productName + "\" đã kết thúc. Vui lòng kiểm tra và liên hệ với người tham gia nếu cần.\n\nTrân trọng.";
        sendNotification(seller.getCustomerId(), sellerMessage, auction);
        emailService.sendEmail(seller.getEmail(), "Thông báo kết thúc phiên đấu giá", sellerEmailText);

        // Gửi thông báo & email cho các người tham gia (không phải người bán)
        for (Customer participant : participants) {
            if (participant.getCustomerId().equals(seller.getCustomerId())) {
                continue;
            }
            if (participant.getCustomerId().equals(winner.getCustomerId())) {
                // Người thắng
                String winnerMessage = String.format("%s - Chúc mừng, bạn đã thắng đấu giá! Vui lòng thanh toán số tiền còn lại: %s VNĐ.",
                        auctionInfo, amountToPay.toPlainString());
                String winnerEmailText = "Kính chào,\n\nChúc mừng bạn đã thắng đấu giá cho sản phẩm \"" + productName + "\".\nSố tiền cần thanh toán là: "
                        + amountToPay.toPlainString() + " VNĐ.\nVui lòng thanh toán trong thời gian quy định.\n\nTrân trọng.";
                sendNotification(participant.getCustomerId(), winnerMessage, auction);
                emailService.sendEmail(participant.getEmail(), "Chúc mừng! Bạn đã thắng đấu giá", winnerEmailText);
            } else {
                // Người không thắng
                String loserMessage = auctionInfo + " - Phiên đấu giá đã kết thúc. Cảm ơn bạn đã tham gia đấu giá.";
                String loserEmailText = "Kính chào,\n\nRất tiếc, bạn không chiến thắng phiên đấu giá cho sản phẩm \"" + productName + "\".\nTiền đặt cọc sẽ được hoàn trả trong thời gian sớm nhất.\n\nTrân trọng.";
                sendNotification(participant.getCustomerId(), loserMessage, auction);
                emailService.sendEmail(participant.getEmail(), "Thông báo kết thúc phiên đấu giá", loserEmailText);
            }
        }
    }
}
