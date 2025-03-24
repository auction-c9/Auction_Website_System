package com.example.auction_management.repository;

import com.example.auction_management.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByCustomerCustomerId(Integer customerId);
    @Query("SELECT n FROM Notification n JOIN FETCH n.customer WHERE n.customer.customerId = :customerId ORDER BY n.timestamp DESC")
    List<Notification> findByCustomerCustomerIdOrderByTimestampDesc(@Param("customerId") Integer customerId);

    List<Notification> findByCustomer_CustomerIdAndAuction_AuctionId(Integer customerId, Integer auctionId);
}
