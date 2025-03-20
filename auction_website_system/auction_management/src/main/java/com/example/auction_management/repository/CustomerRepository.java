package com.example.auction_management.repository;


import com.example.auction_management.model.Account;
import com.example.auction_management.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    @Query("SELECT c.customerId FROM customers c WHERE c.account.username = :username")
    Optional<Integer> findCustomerIdByUsername(@Param("username") String username);
    Optional<Customer> findByAccountUsername(String username);
    Optional<Customer> findByAccount(Account account);
    Optional<Customer> findByEmail(String email);
    @Query("SELECT c FROM customers c WHERE c.account.role.name <> 'ROLE_ADMIN'")
    Page<Customer> findAllNonAdminCustomers(Pageable pageable);

    Optional<Customer> findByAccount_AccountId(Integer accountId);
}
