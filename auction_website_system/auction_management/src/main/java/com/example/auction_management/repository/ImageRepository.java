package com.example.auction_management.repository;

import com.example.auction_management.model.Image;
import com.example.auction_management.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Integer> {
    void deleteByProduct(Product product);

}
