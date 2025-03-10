package com.example.auction_management.repository;

import com.example.auction_management.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction,Integer> {
}
