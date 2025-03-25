package com.example.auction_management.repository;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.AuctionRegistration;
import com.example.auction_management.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuctionRegistrationRepository extends JpaRepository<AuctionRegistration,Integer> {
    List<AuctionRegistration> findByCustomer_CustomerId(Integer customerId);
    Optional<AuctionRegistration> findByCustomer_CustomerIdAndAuction_AuctionId(Integer customerId, Integer auctionId);
    boolean existsByAuctionAndCustomer(Auction auction, Customer customer);
    List<AuctionRegistration> findByAuction_AuctionId(Integer auctionId);

    @Query(
            value = "SELECT ar FROM AuctionRegistration ar JOIN ar.auction a LEFT JOIN a.product p WHERE ar.customer.customerId = :customerId",
            countQuery = "SELECT COUNT(ar) FROM AuctionRegistration ar WHERE ar.customer.customerId = :customerId"
    )
    Page<AuctionRegistration> findByCustomerIdWithBids(@Param("customerId") Integer customerId, Pageable pageable);
}