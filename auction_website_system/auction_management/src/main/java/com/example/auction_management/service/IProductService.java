package com.example.auction_management.service;

import com.example.auction_management.dto.ProductDTO;
import com.example.auction_management.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IProductService extends IService<Product, Integer> {
    Product createProduct(ProductDTO dto);


    Page<Product> getProducts(Pageable pageable);
}
