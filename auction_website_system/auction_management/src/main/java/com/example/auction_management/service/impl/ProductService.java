package com.example.auction_management.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.auction_management.config.CloudinaryConfig;
import com.example.auction_management.dto.ProductDTO;
import com.example.auction_management.exception.ProductCreationException;
import com.example.auction_management.model.*;
import com.example.auction_management.repository.*;
import com.example.auction_management.service.IProductService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService implements IProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private final Cloudinary cloudinary = CloudinaryConfig.getCloudinary();

    @Override
    public List<Product> findAll() {
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
        productRepository.findById(id).ifPresent(product -> {
            product.setIsDeleted(true);
            productRepository.save(product);
        });
    }

    @Transactional
    @Override
    public Product createProduct(ProductDTO dto) {
        try {
            Account account = getAuthenticatedAccount();
            Category category = getCategory(dto.getCategoryId());

            Product product = buildProduct(dto, account, category);
            product = productRepository.save(product);

            uploadDetailImages(dto.getImageFiles(), product);
            createAuction(dto, product);

            return product;
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Lỗi khi tạo sản phẩm: {}", e.getMessage(), e);
            throw new ProductCreationException("Lỗi tạo sản phẩm: " + e.getMessage(), e);
        }
    }

    private Account getAuthenticatedAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new ProductCreationException("Không tìm thấy tài khoản với username: " + username));
    }

    private Category getCategory(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProductCreationException("Không tìm thấy danh mục với id: " + categoryId));
    }

    private Product buildProduct(ProductDTO dto, Account account, Category category) throws IOException {
        Product product = new Product();
        product.setName(dto.getName());
        product.setCategory(category);
        product.setDescription(dto.getDescription());
        product.setBasePrice(dto.getBasePrice());
        product.setIsDeleted(false);
        product.setAccount(account);
        product.setImage(uploadFile(dto.getImageFile()));
        return product;
    }

    private void uploadDetailImages(List<MultipartFile> imageFiles, Product product) throws IOException {
        for (MultipartFile file : imageFiles) {
            if (!file.isEmpty()) {
                String imageUrl = uploadFile(file);
                Image image = new Image();
                image.setImageUrl(imageUrl);
                image.setProduct(product);
                imageRepository.save(image);
                product.getImages().add(image);
            }
        }
    }

    private void createAuction(ProductDTO dto, Product product) {
        Auction auction = new Auction();
        auction.setProduct(product);
        auction.setAuctionStartTime(dto.getAuctionStartTime());
        auction.setAuctionEndTime(dto.getAuctionEndTime());
        auction.setBidStep(dto.getBidStep());
        auction.setCurrentPrice(dto.getBasePrice());
        auction.setStatus(Auction.AuctionStatus.valueOf(dto.getStatus()));
        auctionRepository.save(auction);
    }

    private String uploadFile(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        return uploadResult.get("secure_url").toString();
    }
}
