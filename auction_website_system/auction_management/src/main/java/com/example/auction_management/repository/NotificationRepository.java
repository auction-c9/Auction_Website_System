package com.example.auction_management.repository;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByCustomerCustomerId(Integer customerId);
    @Query("SELECT n FROM Notification n JOIN FETCH n.customer c WHERE c.customerId = :customerId ORDER BY n.timestamp DESC")
    List<Notification> findByCustomer_CustomerIdOrderByTimestampDesc(@Param("customerId") Integer customerId);

    List<Notification> findByCustomer_CustomerIdAndAuction_AuctionId(Integer customerId, Integer auctionId);

    void deleteByAuction(Auction auction);
    @Modifying
    @Query("update Notification n set n.isRead = true where n.customer.customerId = :customerId and n.auction.auctionId = :auctionId")
    void updateIsReadByCustomerAndAuction(@Param("customerId") Integer customerId, @Param("auctionId") Integer auctionId);
}
