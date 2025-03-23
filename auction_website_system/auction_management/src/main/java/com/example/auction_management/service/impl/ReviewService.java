package com.example.auction_management.service.impl;

import com.example.auction_management.dto.CreateReviewDTO;
import com.example.auction_management.dto.ReviewDTO;
import com.example.auction_management.exception.ResourceNotFoundException;
import com.example.auction_management.model.*;
import com.example.auction_management.repository.AccountRepository;
import com.example.auction_management.repository.BidRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.ReviewRepository;
import com.example.auction_management.service.IReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {
    private final ReviewRepository reviewRepository;
    private final BidRepository bidRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public ReviewDTO createReview(CreateReviewDTO dto, String username) {
        // Lấy thông tin người mualsdfdsfe4oi898909
        Customer buyer = customerRepository.findByAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Kiểm tra bid hợp lệ
        Bid bid = bidRepository.findById(dto.getBidId())
                .orElseThrow(() -> new RuntimeException("Bid not found"));

        if (!bid.getIsWinner() || !bid.getAuction().getStatus().equals(Auction.AuctionStatus.ended)) {
            throw new RuntimeException("Không thể đánh giá phiên đấu giá này");
        }

        // Kiểm tra đã tồn tại review cho bid này chưa
        if (reviewRepository.existsByBid_BidId(bid.getBidId())) {
            throw new RuntimeException("Bạn đã đánh giá phiên đấu giá này");
        }

        // Tạo review
        Review review = new Review();
        review.setSeller(bid.getAuction().getProduct().getAccount().getCustomer());
        review.setBuyer(buyer);
        review.setBid(bid);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        Review savedReview = reviewRepository.save(review);

        // Cập nhật average rating cho seller
        updateSellerRating(review.getSeller());

        return convertToDTO(savedReview);
    }

    private void updateSellerRating(Customer seller) {
        List<Review> reviews = reviewRepository.findBySeller_CustomerId(seller.getCustomerId());
        BigDecimal average = reviews.stream()
                .map(r -> BigDecimal.valueOf(r.getRating()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);
        seller.setAverageRating(average);
        customerRepository.save(seller);
    }

    public List<ReviewDTO> getReviewsBySellerId(Integer sellerId) {
        return reviewRepository.findBySeller_CustomerId(sellerId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    private ReviewDTO convertToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setBuyerName(review.getBuyer().getName());
        dto.setProductName(review.getBid().getAuction().getProduct().getName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }

}
