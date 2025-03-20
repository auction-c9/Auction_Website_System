package com.example.auction_management.repository;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Integer> {
    Optional<Account> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT p FROM Product p JOIN FETCH p.account WHERE p.productId = :id")
    Product findProductWithAccount(@Param("id") Integer id);

}
