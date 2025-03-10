package com.example.auction_management.controller;

import com.example.auction_management.dto.ProductDTO;
import com.example.auction_management.model.Product;
import com.example.auction_management.service.impl.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // Lấy danh sách sản phẩm (chỉ lấy sản phẩm chưa bị xóa mềm)
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.findAll();
        return ResponseEntity.ok(products);
    }

    // Lấy sản phẩm theo id
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Integer id) {
        Optional<Product> productOpt = productService.findById(id);
        return productOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Tạo mới sản phẩm
    // Sử dụng @ModelAttribute để xử lý dữ liệu form bao gồm file upload
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @ModelAttribute ProductDTO productDTO) {
        Product product = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    // Cập nhật sản phẩm
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Integer id,
                                                 @Valid @ModelAttribute ProductDTO productDTO) {
        Optional<Product> productOpt = productService.findById(id);
        if (productOpt.isPresent()) {
            productService.updateProduct(productOpt.get(), productDTO);
            return ResponseEntity.ok(productOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // Xóa mềm sản phẩm theo id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        try {
            productService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
