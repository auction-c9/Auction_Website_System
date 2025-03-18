package com.example.auction_management.controller;

import com.example.auction_management.dto.ChatMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // User gửi tin nhắn đến Admin
    @MessageMapping("/private-message")
    public void sendToAdmin(@Payload ChatMessageDTO message) {
        System.out.println("📩 Tin nhắn từ user ID: " + message.getSenderId() + " gửi đến admin: " + message.getContent());
        messagingTemplate.convertAndSend("/topic/admin/messages", message);
    }

    // Admin gửi tin nhắn đến User
    @MessageMapping("/admin-reply")
    public void sendToUser(@Payload ChatMessageDTO message) {
        System.out.println("📩 Admin ID: " + message.getSenderId() + " gửi tin nhắn đến user ID: " + message.getReceiverId());
        messagingTemplate.convertAndSendToUser(
                message.getReceiverId().toString(), "/queue/private-messages", message
        );
    }
}

