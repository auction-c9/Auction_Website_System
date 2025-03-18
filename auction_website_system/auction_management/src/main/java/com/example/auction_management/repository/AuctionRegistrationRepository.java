package com.example.auction_management.repository;

import com.example.auction_management.model.AuctionRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRegistrationRepository extends JpaRepository<AuctionRegistration,Integer> {
}
