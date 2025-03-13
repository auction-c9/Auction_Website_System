package com.example.auction_management.service;

import com.example.auction_management.dto.ProductDTO;
import com.example.auction_management.model.Product;

public interface IProductService extends IService<Product, Integer> {
    Product createProduct(ProductDTO dto);

}
