package com.example.auction_management.service;

import com.example.auction_management.model.*;
import com.example.auction_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FollowRepository followRepository;

    @Async
    public void notifyNewProduct(Product product) {
        try {
            Customer seller = product.getAccount().getCustomer();
            String message = String.format("Tài khoản %s vừa đăng sản phẩm mới: %s",
                    seller.getName(), product.getName());

            List<Follow> followers = followRepository.findBySeller(seller);

            followers.forEach(follow -> {
                // Tạo và lưu notification
                Notification notification = new Notification();
                notification.setCustomer(follow.getFollower());
                notification.setMessage(message);
                notification.setTimestamp(LocalDateTime.now());
                notificationRepository.save(notification);

                // Gửi qua WebSocket
                messagingTemplate.convertAndSendToUser(
                        follow.getFollower().getAccount().getUsername(),
                        "/queue/notifications",
                        notification
                );
            });
        } catch (Exception e) {
            logger.error("Error sending product notifications: {}", e.getMessage());
        }
    }
    /**
     * Gửi thông báo cho userId với nội dung message.
     * Thêm log để theo dõi quá trình tìm customer, lưu notification và gửi WebSocket.
     */
    public void sendNotification(Integer userId, String message) {
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

        // Lưu notification vào DB
        notificationRepository.save(notification);
        logger.debug("Notification saved to DB with ID: {}", notification.getNotification_id());

        // Gửi notification qua WebSocket
        String username = customer.getAccount().getUsername();
        logger.debug("Sending notification to username: {} at /queue/notifications", username);

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                notification
        );
        logger.info("Notification sent via WebSocket to user: {}", username);
    }

    /**
     * Gửi thông báo đặt giá (bid) cho tất cả người tham gia và người bán.
     */
    public void sendBidNotification(Integer auctionId, Integer bidderId, String message) {
        logger.info("sendBidNotification() - auctionId: {}, bidderId: {}, message: {}",
                auctionId, bidderId, message);

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

        // Gửi thông báo cho tất cả người tham gia (trừ bidder)
        for (Customer customer : participants) {
            logger.debug("Participant ID: {}, username: {}",
                    customer.getCustomerId(), customer.getAccount().getUsername());
            if (!customer.getCustomerId().equals(bidderId)) {
                sendNotification(customer.getCustomerId(), message);
            }
        }

        // Gửi thông báo cho người bán (nếu seller không phải bidder)
        if (!seller.getCustomerId().equals(bidderId)) {
            String sellerMessage = "Có người đặt giá đấu giá thành công cho sản phẩm của bạn!";
            logger.debug("Sending seller notification: {}", sellerMessage);
            sendNotification(seller.getCustomerId(), sellerMessage);
        }
    }

    /**
     * Lấy người bán của một phiên đấu giá theo cách giống BidService.
     */
    private Customer getAuctionSeller(Auction auction) {
        return auction.getProduct().getAccount().getCustomer();
    }

    /**
     * Lấy danh sách thông báo của một customer theo thứ tự thời gian giảm dần.
     */
    public List<Notification> getNotificationsByCustomerId(Integer customerId) {
        logger.info("getNotificationsByCustomerId() - customerId: {}", customerId);
        List<Notification> notifications = notificationRepository.findByCustomer_CustomerIdOrderByTimestampDesc(customerId);
        logger.debug("Found {} notifications for customerId {}", notifications.size(), customerId);
        return notifications;
    }
}
