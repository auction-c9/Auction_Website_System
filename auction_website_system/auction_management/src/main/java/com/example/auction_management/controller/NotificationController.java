package com.example.auction_management.controller;

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
    public List<Notification> getNotifications(@PathVariable Integer customerId) {
        return notificationService.getNotificationsByCustomerId(customerId);
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
    public ResponseEntity<Void> markNotificationsAsRead(
            @PathVariable Integer customerId,
            @PathVariable Integer auctionId) {
        notificationService.markAsRead(customerId, auctionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendNotification(@RequestParam Integer customerId, @RequestParam String message, @RequestParam Integer auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        notificationService.sendNotification(customerId, message, auction);
        return ResponseEntity.ok().build();
    }
}
