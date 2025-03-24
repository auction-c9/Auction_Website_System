package com.example.auction_management.repository;


import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    Optional<Transaction> findById(Integer id); // Nếu không có transactionId, dùng id
    Optional<Transaction> findByTransactionId(String transactionId);
    Transaction findByVnpTxnRef(String vnpTxnRef);
    boolean existsByCustomer_CustomerIdAndAuction_AuctionIdAndStatus(Integer customerId, Integer auctionId, String status);
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.status = 'SUCCESS' " +
            "AND t.auction.auctionEndTime BETWEEN :yesterday AND :now " +
            "AND NOT EXISTS ( " +
            "    SELECT 1 FROM Bid b " +
            "    WHERE b.auction = t.auction " +
            "    AND b.customer = t.customer " +
            "    AND b.isWinner = true " +
            ")")
    List<Transaction> findFailedDeposits(@Param("yesterday") LocalDateTime yesterday,
                                         @Param("now") LocalDateTime now);

    @Query(value = "SELECT DATE(created_at) AS transactionDate, SUM(amount) AS totalAmount " +
            "FROM transactions " +
            "WHERE created_at >= CURDATE() - INTERVAL :days DAY " +
            "AND status = 'SUCCESS' " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY DATE(created_at) ASC",
            nativeQuery = true)
    List<Object[]> sumTransactionsByDay(@Param("days") int days);
    void deleteByAuction(Auction auction);

}
