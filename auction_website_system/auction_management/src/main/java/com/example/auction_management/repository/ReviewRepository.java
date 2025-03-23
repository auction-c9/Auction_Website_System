package com.example.auction_management.repository;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Bid;
import com.example.auction_management.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ReviewRepository extends JpaRepository<Review,Integer> {
    List<Review> findBySeller_CustomerId(Integer sellerId);
    boolean existsByBid_BidId(Integer bidId);

    @Query("SELECT r FROM Review r JOIN FETCH r.bid b JOIN FETCH b.auction a JOIN FETCH a.product WHERE r.id = :id")
    Optional<Review> findByIdWithRelations(@Param("id") Integer id);

    @Query("SELECT r.bid.bidId FROM Review r WHERE r.buyer.customerId = :customerId")
    Set<Integer> findReviewedBidIdsByCustomerId(@Param("customerId") Integer customerId);
}
