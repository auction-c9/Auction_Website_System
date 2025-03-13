package com.example.auction_management.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @MessageMapping("/chat.sendMessage") // Endpoint nhận tin nhắn
    @SendTo("/topic/public") // Gửi tin nhắn đến tất cả người dùng
    public String sendMessage(String message) {
        return message; // Trả về tin nhắn để broadcast
    }
}