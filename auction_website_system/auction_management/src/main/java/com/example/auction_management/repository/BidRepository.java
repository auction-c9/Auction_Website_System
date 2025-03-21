package com.example.auction_management.repository;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Integer> {
    Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction);
    List<Bid> findByAuction(Auction auction);

    List<Bid> findByAuction_AuctionIdOrderByBidAmountDesc(Integer auctionId);
    @Query("SELECT b FROM Bid b WHERE b.auction.auctionId = :auctionId")
    List<Bid> findByAuctionId(@Param("auctionId") Integer auctionId);
    List<Bid> findByCustomer_CustomerIdOrderByBidTimeDesc(Integer customerId);
}
