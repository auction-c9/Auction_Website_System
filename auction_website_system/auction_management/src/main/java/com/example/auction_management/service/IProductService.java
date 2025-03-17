package com.example.auction_management.service;

import com.example.auction_management.dto.ProductDTO;
import com.example.auction_management.model.Product;

import java.util.List;

public interface IProductService extends IService<Product, Integer> {
    Product createProduct(ProductDTO dto);


}
