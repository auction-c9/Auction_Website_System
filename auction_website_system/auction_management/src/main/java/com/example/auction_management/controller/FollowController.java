package com.example.auction_management.controller;

import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Follow;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.FollowRepository;
import com.example.auction_management.service.ICustomerService;
import com.example.auction_management.service.IFollowService;
import com.example.auction_management.service.NotificationService;
import com.example.auction_management.service.impl.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {
    private final IFollowService followService;
    private final ICustomerService customerService;

    @PostMapping("/{sellerId}")
    public ResponseEntity<?> followSeller(@PathVariable Integer sellerId,
                                          Authentication authentication) {
        Customer follower = customerService.getCurrentCustomer(authentication);
        followService.followSeller(sellerId, follower);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Theo dõi thành công"
        ));
    }

    @DeleteMapping("/{sellerId}")
    public ResponseEntity<?> unfollowSeller(@PathVariable Integer sellerId,
                                            Authentication authentication) {
        Customer follower = customerService.getCurrentCustomer(authentication);
        followService.unfollowSeller(sellerId, follower);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Hủy theo dõi thành công"
        ));
    }

    @GetMapping("/check/{sellerId}")
    public ResponseEntity<?> checkFollowStatus(@PathVariable Integer sellerId,
                                               Authentication authentication) {
        Customer follower = customerService.getCurrentCustomer(authentication);
        boolean isFollowing = followService.checkFollowStatus(sellerId, follower);
        return ResponseEntity.ok(Map.of(
                "isFollowing", isFollowing
        ));
    }
}
