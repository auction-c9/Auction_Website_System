package com.example.auction_management.controller;

import com.example.auction_management.dto.ProductDTO;
import com.example.auction_management.model.Product;
import com.example.auction_management.service.impl.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class); // Khai báo logger

    @Autowired
    private ProductService productService;

    // Lấy tất cả sản phẩm
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.findAll());
    }

    // Lấy sản phẩm theo ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Integer id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Thêm mới sản phẩm
    @PostMapping("/create")
    public ResponseEntity<?> createProduct(@Valid @ModelAttribute ProductDTO productDTO) {
        Product product = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Tạo sản phẩm thành công",
                "productId", product.getProductId(),
                "productName", product.getName()
        ));
    }


    // Xóa mềm sản phẩm
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
