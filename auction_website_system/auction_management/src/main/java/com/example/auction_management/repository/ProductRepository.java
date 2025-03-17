package com.example.auction_management.repository;

import com.example.auction_management.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByAccountUsername(String username);
}
