package com.example.auction_management.config;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Review;
import com.example.auction_management.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewSecurity {
    private final ReviewRepository reviewRepository;

    public boolean isReviewOwner(Integer reviewId, Account account) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        log.debug("Checking review ownership for account: {}", account.getAccountId());
        return review.getBuyer().getAccount().equals(account.getAccountId());
    }
}
