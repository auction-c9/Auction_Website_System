package com.example.auction_management.repository;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    @Query("SELECT c.customerId FROM customers c WHERE c.account.username = :username")
    Optional<Integer> findCustomerIdByUsername(@Param("username") String username);
}
