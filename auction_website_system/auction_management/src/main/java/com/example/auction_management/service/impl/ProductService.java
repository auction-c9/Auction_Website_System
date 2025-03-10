package com.example.auction_management.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.auction_management.config.CloudinaryConfig;
import com.example.auction_management.dto.ProductDTO;
import com.example.auction_management.model.Category;
import com.example.auction_management.model.Product;
import com.example.auction_management.repository.CategoryRepository;
import com.example.auction_management.repository.ProductRepository;
import com.example.auction_management.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService implements IProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // Khởi tạo Cloudinary (giả sử bạn đã cấu hình CloudinaryConfig tương tự như PostService)
    private Cloudinary cloudinary = CloudinaryConfig.getCloudinary();

    @Override
    public List<Product> findAll() {
        // Lấy tất cả sản phẩm chưa bị xóa mềm
        return productRepository.findAll().stream()
                .filter(product -> product.getIsDeleted() == null || !product.getIsDeleted())
                .toList();
    }

    @Override
    public Optional<Product> findById(Integer id) {
        return productRepository.findById(id);
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public void deleteById(Integer id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setIsDeleted(true);  // xóa mềm thay vì xóa hoàn toàn
            productRepository.save(product);
        } else {
            throw new RuntimeException("Không tìm thấy sản phẩm với id: " + id);
        }
    }

    // Phương thức tạo sản phẩm mới tương tự như createPost trong PostService
    public Product createProduct(ProductDTO productDTO) {
        try {
            Product product = new Product();
            product.setName(productDTO.getName());

            // Lấy Category dựa vào categoryId trong DTO
            Optional<Category> categoryOpt = categoryRepository.findById(productDTO.getCategoryId());
            if (!categoryOpt.isPresent()) {
                throw new RuntimeException("Không tìm thấy danh mục với id: " + productDTO.getCategoryId());
            }
            product.setCategory(categoryOpt.get());

            product.setDescription(productDTO.getDescription());
            product.setBasePrice(productDTO.getBasePrice());
            product.setIsDeleted(false);

            // Xử lý upload ảnh qua Cloudinary nếu file ảnh không rỗng
            MultipartFile imageFile = productDTO.getImageFile();
            if (imageFile != null && !imageFile.isEmpty()) {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(imageFile.getBytes(), ObjectUtils.emptyMap());
                String imageUrl = uploadResult.get("secure_url").toString();
                product.setImage(imageUrl);
            }
            return productRepository.save(product);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi upload ảnh lên Cloudinary: " + e.getMessage());
        }
    }

    // Phương thức cập nhật sản phẩm tương tự như updatePost
    public void updateProduct(Product product, ProductDTO productDTO) {
        try {
            product.setName(productDTO.getName());

            Optional<Category> categoryOpt = categoryRepository.findById(productDTO.getCategoryId());
            if (!categoryOpt.isPresent()) {
                throw new RuntimeException("Không tìm thấy danh mục với id: " + productDTO.getCategoryId());
            }
            product.setCategory(categoryOpt.get());

            product.setDescription(productDTO.getDescription());
            product.setBasePrice(productDTO.getBasePrice());

            // Nếu có file ảnh mới, tiến hành upload và cập nhật
            MultipartFile imageFile = productDTO.getImageFile();
            if (imageFile != null && !imageFile.isEmpty()) {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(imageFile.getBytes(), ObjectUtils.emptyMap());
                String imageUrl = uploadResult.get("secure_url").toString();
                product.setImage(imageUrl);
            }
            productRepository.save(product);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi upload ảnh lên Cloudinary: " + e.getMessage());
        }
    }
}
