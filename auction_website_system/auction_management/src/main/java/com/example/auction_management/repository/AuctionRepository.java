package com.example.auction_management.repository;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Auction.AuctionStatus;
import com.example.auction_management.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, Integer> {
    // Tìm theo trạng thái
    List<Auction> findByStatus(AuctionStatus status);

    // Tìm theo sản phẩm
    List<Auction> findByProduct(Product product);

    // Tìm các phiên đang diễn ra
    @Query("SELECT a FROM Auction a WHERE a.auctionStartTime <= :now AND a.auctionEndTime >= :now AND a.status = 'active'")
    List<Auction> findOngoingAuctions(LocalDateTime now);
}
