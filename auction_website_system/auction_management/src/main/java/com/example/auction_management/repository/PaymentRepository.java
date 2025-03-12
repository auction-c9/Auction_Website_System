package com.example.auction_management.repository;

import com.example.auction_management.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
//    Optional<Payment> findByAuctionIdAndPaymentType(Integer auctionId, String paymentType);

}
