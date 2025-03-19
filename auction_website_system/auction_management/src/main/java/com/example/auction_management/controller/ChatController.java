package com.example.auction_management.controller;

import com.example.auction_management.model.ChatMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class ChatController {
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // 1️⃣ User gửi tin cho Admin
    @MessageMapping("/sendToAdmin")
    public void sendToAdmin(ChatMessage message, Principal principal) {
        String username = principal.getName();
        message.setSender(username);
        message.setReceiver("admin");
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSendToUser("admin", "/queue/messages", message);
        messagingTemplate.convertAndSendToUser(username, "/queue/messages", message);
    }

    // 2️⃣ Admin gửi tin nhắn riêng tư cho User cụ thể
    @MessageMapping("/sendToUser")
    public void sendToUser(ChatMessage message) {
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(message.getReceiver(), "/queue/messages", message);
        messagingTemplate.convertAndSendToUser("admin", "/queue/messages", message);
    }
}
