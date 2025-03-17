package com.example.auction_management.repository;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Auction.AuctionStatus;
import com.example.auction_management.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Integer> {
    // Tìm theo trạng thái
    List<Auction> findByStatus(AuctionStatus status);

    // Tìm theo sản phẩm
    Optional<Auction> findByProduct(Product product);

    // Tìm các phiên đấu giá theo username của chủ sản phẩm
    List<Auction> findByProduct_Account_Username(String username);

    // Tìm các phiên đang diễn ra
    @Query("SELECT a FROM Auction a WHERE a.auctionStartTime <= :now AND a.auctionEndTime >= :now AND a.status = 'active'")
    List<Auction> findOngoingAuctions(LocalDateTime now);
}
