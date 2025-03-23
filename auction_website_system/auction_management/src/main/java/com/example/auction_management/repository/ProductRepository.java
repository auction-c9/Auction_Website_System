package com.example.auction_management.repository;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByAccountUsername(String username);
    List<Product> findByAccountAndIsDeletedFalse(Account account);
    List<Product> findAllByIsDeletedFalse();
    Optional<Product> findByProductIdAndIsDeletedFalse(Integer id);
    Page<Product> findByIsDeletedFalse(Pageable pageable);

}
