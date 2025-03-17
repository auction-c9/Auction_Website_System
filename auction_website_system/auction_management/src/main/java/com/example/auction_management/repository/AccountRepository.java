package com.example.auction_management.repository;

import com.example.auction_management.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Integer> {
    Optional<Account> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT a.role.name FROM accounts a WHERE a.username = :username")
    Optional<String> findRoleNameByUsername(@Param("username") String username);
}
