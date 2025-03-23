package com.example.auction_management.repository;

import com.example.auction_management.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByCustomerCustomerId(Integer customerId);
    List<Notification> findByCustomer_CustomerIdOrderByTimestampDesc(Integer customerId);
}
