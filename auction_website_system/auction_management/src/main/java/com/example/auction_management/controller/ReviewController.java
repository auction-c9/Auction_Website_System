package com.example.auction_management.controller;

import com.example.auction_management.dto.CreateReviewDTO;
import com.example.auction_management.dto.ReviewDTO;
import com.example.auction_management.model.Account;
import com.example.auction_management.model.Review;
import com.example.auction_management.service.impl.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(@Valid @RequestBody CreateReviewDTO dto, Authentication authentication) {
        ReviewDTO createdReview = reviewService.createReview(dto, authentication.getName());
        return ResponseEntity.ok(createdReview);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsBySeller(@PathVariable Integer sellerId) {
        List<ReviewDTO> reviews = reviewService.getReviewsBySellerId(sellerId);
        return ResponseEntity.ok(reviews);
    }
}
