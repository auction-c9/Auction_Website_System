package com.example.auction_management.repository;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Auction.AuctionStatus;
import com.example.auction_management.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Integer> {

    List<Auction> findByStatus(AuctionStatus status);

    Optional<Auction> findByProduct(Product product);

    List<Auction> findByProduct_Account_Username(String username);

    @Query("SELECT a FROM Auction a WHERE a.auctionStartTime <= :now AND a.auctionEndTime >= :now AND a.status = 'active'")
    List<Auction> findOngoingAuctions(LocalDateTime now);

    List<Auction> findAllByProduct(Product product);

    List<Auction> findAllByIsDeletedFalse();

    Optional<Auction> findByAuctionIdAndIsDeletedFalse(Integer auctionId);

    Optional<Auction> findByProductAndIsDeletedFalse(Product product);

    List<Auction> findByAuctionEndTimeBeforeAndWinnerNotifiedFalse(LocalDateTime endTime);


    @Modifying
    @Query("UPDATE Auction a SET a.winnerNotified = true WHERE a.auctionId = :auctionId")
    void markWinnerNotified(@Param("auctionId") Integer auctionId);


    @Transactional
    @Modifying
    @Query("UPDATE Auction a SET a.status = " +
            "CASE " +
            "WHEN a.auctionStartTime > :now THEN 'PENDING' " +
            "WHEN a.auctionStartTime <= :now AND a.auctionEndTime >= :now THEN 'ACTIVE' " +
            "ELSE 'ENDED' " +
            "END")
    void updateAuctionStatuses(@Param("now") LocalDateTime now);
    @Query("SELECT a FROM Auction a JOIN FETCH a.product p " +
            "WHERE p.account.accountId = :accountId " +
            "AND a.isDeleted = false " +
            "AND p.isDeleted = false")
    List<Auction> findActiveAuctionsByAccountId(@Param("accountId") Integer accountId);

    List<Auction> findByProductInAndIsDeletedFalse(List<Product> products);

    @Query("SELECT MONTH(a.auctionStartTime) AS month, COUNT(a) AS count " +
            "FROM Auction a " +
            "WHERE YEAR(a.auctionStartTime) = YEAR(CURRENT_DATE) " +
            "GROUP BY MONTH(a.auctionStartTime) " +
            "ORDER BY MONTH(a.auctionStartTime)")
    List<Object[]> countAuctionsByMonth();

}
