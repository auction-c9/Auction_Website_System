package com.example.auction_management.repository;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    Optional<Transaction> findById(Integer id); // Nếu không có transactionId, dùng id
    Optional<Transaction> findByTransactionId(String transactionId);
    Transaction findByVnpTxnRef(String vnpTxnRef);
    boolean existsByCustomerAndAuctionAndStatus(Customer customer, Auction auction, String status);
    Transaction findFirstByCustomerAndAuctionAndAmountAndStatusOrderByCreatedAtDesc(
            Customer customer, Auction auction, Double amount, String status);


}
