package com.example.auction_management.repository;

import com.example.auction_management.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Integer> {
    Optional<Account> findByUsername(String username);
}
