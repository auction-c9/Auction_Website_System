package com.example.auction_management.repository;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByAccountUsername(String username);
    List<Product> findByAccountAndIsDeletedFalse(Account account);

}
