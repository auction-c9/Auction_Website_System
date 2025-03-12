package com.example.auction_management.controller;

import com.example.auction_management.dto.BidDTO;
import com.example.auction_management.dto.BidResponseDTO;
import com.example.auction_management.service.impl.BidService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BidController {

    private final BidService bidService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody BidDTO bidDTO, HttpServletRequest request, Authentication authentication) {
        System.out.println("Received bid request: " + bidDTO); // Debug log
        System.out.println("Token received: " + request.getHeader("Authorization")); // Kiểm tra token

        // Kiểm tra xác thực
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn cần đăng nhập để đặt giá!"));
        }

        if (bidDTO.getCustomerId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Customer ID is missing!"));
        }

        try {
            BidResponseDTO bidResponse = bidService.placeBid(bidDTO);
            return ResponseEntity.ok(Map.of(
                    "message", "Đặt giá thành công!",
                    "bid", bidResponse
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
