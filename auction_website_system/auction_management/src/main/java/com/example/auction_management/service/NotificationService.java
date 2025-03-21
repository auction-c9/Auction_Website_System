package com.example.auction_management.service;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Notification;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.repository.BidRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final SimpMessagingTemplate messagingTemplate; // Tiêm SimpMessagingTemplate

    public void sendNotification(Integer userId, String message) {
        // Lấy Customer từ CSDL theo id
        Customer customer = customerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Notification notification = new Notification();
        notification.setCustomer(customer);
        notification.setMessage(message);
        notification.setTimestamp(LocalDateTime.now());

        notificationRepository.save(notification);

        // Gửi thông báo realtime qua WebSocket
        // Giả sử rằng account username của customer là định danh cho WebSocket user
        messagingTemplate.convertAndSendToUser(
                customer.getAccount().getUsername(), // Định danh người dùng
                "/queue/notifications",              // Đường dẫn destination dành riêng cho user
                notification
        );
    }
    public void sendBidNotification(Integer auctionId, Integer bidderId, String message) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        Customer bidder = customerRepository.findById(bidderId)
                .orElseThrow(() -> new RuntimeException("Bidder not found"));

        // ✅ Lấy người bán theo cùng logic của BidService
        Customer seller = getAuctionSeller(auction);

        // Lấy danh sách tất cả những người đã tham gia đấu giá
        List<Customer> participants = bidRepository.findDistinctCustomersByAuctionId(auctionId);

        // Gửi thông báo đến tất cả người tham gia (trừ người đặt giá)
        for (Customer customer : participants) {
            System.out.println("Participant ID: " + customer.getCustomerId());
            if (!customer.getCustomerId().equals(bidderId)) {
                sendNotification(customer.getCustomerId(), message);
            }
        }

        // ✅ Gửi thông báo cho người bán nếu họ không phải là người đặt giá
        if (!seller.getCustomerId().equals(bidderId)) {
            String sellerMessage = "Có người đặt giá đấu giá thành công cho sản phẩm của bạn!";
            sendNotification(seller.getCustomerId(), sellerMessage);
        }
    }

    /**
     * ✅ Lấy người bán của một phiên đấu giá theo cách giống BidService.
     */
    private Customer getAuctionSeller(Auction auction) {
        return auction.getProduct().getAccount().getCustomer();

    }

}
