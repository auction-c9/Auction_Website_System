package com.example.auction_management.repository;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Bid;
import com.example.auction_management.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review,Integer> {
    List<Review> findBySeller_CustomerId(Integer sellerId);
    boolean existsByBid_BidId(Integer bidId);
}
