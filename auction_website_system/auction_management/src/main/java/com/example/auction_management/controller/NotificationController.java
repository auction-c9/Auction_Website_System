package com.example.auction_management.controller;

import com.example.auction_management.dto.AuctionDTO;
import com.example.auction_management.dto.NotificationDTO;
import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Notification;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    private AuctionRepository auctionRepository;
    // Inject SimpMessagingTemplate để gửi tin nhắn qua WebSocket
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/{customerId}")
    public List<NotificationDTO> getNotifications(@PathVariable Integer customerId) {
        List<Notification> notifications = notificationService.getNotificationsByCustomerId(customerId);
        return notifications.stream().map(notification -> {
            AuctionDTO auctionDTO = null;
            if (notification.getAuction() != null) {
                auctionDTO = new AuctionDTO();
                auctionDTO.setAuctionId(notification.getAuction().getAuctionId());
                auctionDTO.setProductName(notification.getAuction().getProduct().getName());
                // Gán thêm các trường khác nếu cần
            }
            return new NotificationDTO(
                    notification.getNotification_id(),
                    notification.getCustomer().getCustomerId(),
                    auctionDTO, // Gán đối tượng AuctionDTO vào đây
                    notification.getMessage(),
                    notification.getTimestamp(),
                    notification.getIsRead()
            );
        }).collect(Collectors.toList());
    }


    @PostMapping("/test-notification")
    public ResponseEntity<?> sendTestNotification(Principal principal) {

        logger.info("Principal name = " + principal.getName());
        // Tạo đối tượng thông báo mẫu với thông điệp và trạng thái isRead = false
        Notification notificationObject = new Notification("Đây là tin nhắn test", false);
        // Gửi thông báo đến user hiện tại qua kênh "/queue/notifications"
        messagingTemplate.convertAndSend("/topic/notifications", notificationObject);
        return ResponseEntity.ok("Notification sent");
    }
    @PutMapping("/read/{customerId}/{auctionId}")
    public ResponseEntity<List<NotificationDTO>> markNotificationsAsRead(
            @PathVariable Integer customerId,
            @PathVariable Integer auctionId) {
        notificationService.markAsRead(customerId, auctionId);
        List<Notification> notifications = notificationService.getNotificationsByCustomerId(customerId);
        List<NotificationDTO> dtos = notifications.stream().map(notification -> {
            AuctionDTO auctionDTO = null;
            if (notification.getAuction() != null) {
                auctionDTO = new AuctionDTO();
                auctionDTO.setAuctionId(notification.getAuction().getAuctionId());
                auctionDTO.setProductName(notification.getAuction().getProduct().getName());
                // Gán thêm các trường khác nếu cần
            }
            return new NotificationDTO(
                    notification.getNotification_id(),
                    notification.getCustomer().getCustomerId(),
                    auctionDTO,  // Đúng kiểu, là AuctionDTO
                    notification.getMessage(),
                    notification.getTimestamp(),
                    notification.getIsRead()
            );

        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }


    @PostMapping("/send")
    public ResponseEntity<Void> sendNotification(@RequestParam Integer customerId, @RequestParam String message, @RequestParam Integer auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        notificationService.sendNotification(customerId, message, auction);
        return ResponseEntity.ok().build();
    }
}
