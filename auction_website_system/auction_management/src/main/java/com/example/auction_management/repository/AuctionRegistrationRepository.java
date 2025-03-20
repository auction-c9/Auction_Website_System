package com.example.auction_management.repository;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.AuctionRegistration;
import com.example.auction_management.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuctionRegistrationRepository extends JpaRepository<AuctionRegistration,Integer> {
    List<AuctionRegistration> findByCustomer_CustomerId(Integer customerId);
    Optional<AuctionRegistration> findByCustomer_CustomerIdAndAuction_AuctionId(Integer customerId, Integer auctionId);
    boolean existsByAuctionAndCustomer(Auction auction, Customer customer);
    List<AuctionRegistration> findByAuction_AuctionId(Integer auctionId);
}
