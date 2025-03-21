package com.example.auction_management.controller;

import com.example.auction_management.model.Notification;
import com.example.auction_management.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/notifications/{customerId}")
    public List<Notification> getNotificationsByCustomer(@PathVariable Integer customerId) {
        return notificationRepository.findByCustomerCustomerId(customerId);
    }
}
