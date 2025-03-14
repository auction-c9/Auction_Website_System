package com.example.auction_management.repository;

import com.example.auction_management.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    Optional<Transaction> findById(Integer id); // Nếu không có transactionId, dùng id
    Optional<Transaction> findByTransactionId(String transactionId);
    Transaction findByVnpTxnRef(String vnpTxnRef);



}
