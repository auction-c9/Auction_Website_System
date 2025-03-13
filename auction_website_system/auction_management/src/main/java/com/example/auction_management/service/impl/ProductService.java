package com.example.auction_management.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.auction_management.config.CloudinaryConfig;
import com.example.auction_management.dto.ProductDTO;
import com.example.auction_management.exception.ProductCreationException;
import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Account;
import com.example.auction_management.model.Category;
import com.example.auction_management.model.Image;
import com.example.auction_management.model.Product;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.repository.AccountRepository;
import com.example.auction_management.repository.CategoryRepository;
import com.example.auction_management.repository.ImageRepository;
import com.example.auction_management.repository.ProductRepository;
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
import java.util.ArrayList;
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

    private Cloudinary cloudinary = CloudinaryConfig.getCloudinary();

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
        Optional<Product> productOpt = productRepository.findById(id);
        productOpt.ifPresent(product -> {
            product.setIsDeleted(true);
            productRepository.save(product);
        });
    }

    @Transactional
    @Override
    public Product createProduct(ProductDTO dto) {
        try {
            logger.info("Bắt đầu tạo sản phẩm với tên: {}", dto.getName());

            // Lấy thông tin tài khoản từ SecurityContextHolder
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName(); // Username của người đăng nhập

            // Truy vấn Account từ database
            Account account = accountRepository.findByUsername(username)
                    .orElseThrow(() -> new ProductCreationException("Không tìm thấy tài khoản với username: " + username));
            logger.info("Người đăng tin: {}", username);

            // Kiểm tra danh mục
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ProductCreationException("Không tìm thấy danh mục với id: " + dto.getCategoryId()));
            logger.info("Tìm thấy danh mục: {} (ID: {})", category.getName(), category.getCategoryId());

            // Tạo mới Product và gán thông tin người đăng tin
            Product product = new Product();
            product.setName(dto.getName());
            product.setCategory(category);
            product.setDescription(dto.getDescription());
            product.setBasePrice(dto.getBasePrice());
            product.setIsDeleted(false);
            product.setAccount(account);  // Gán thông tin tài khoản đăng tin
            logger.info("Khởi tạo sản phẩm thành công, người đăng: {}", username);

            // Upload ảnh đại diện
            if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
                logger.info("Bắt đầu upload ảnh đại diện: {}", dto.getImageFile().getOriginalFilename());
                String mainImageUrl = uploadFile(dto.getImageFile());
                product.setImage(mainImageUrl);
                logger.info("Upload ảnh đại diện thành công, URL: {}", mainImageUrl);
            } else {
                logger.warn("Không có ảnh đại diện được cung cấp hoặc ảnh rỗng.");
            }

            // Lưu sản phẩm để lấy productId
            product = productRepository.save(product);
            logger.info("Đã lưu sản phẩm vào DB, ID: {}", product.getProductId());

            // Upload và lưu các ảnh chi tiết
            if (dto.getImageFiles() != null && !dto.getImageFiles().isEmpty()) {
                logger.info("Bắt đầu upload {} ảnh chi tiết.", dto.getImageFiles().size());
                for (MultipartFile file : dto.getImageFiles()) {
                    if (!file.isEmpty()) {
                        logger.info("Đang upload ảnh chi tiết: {}", file.getOriginalFilename());
                        String imageUrl = uploadFile(file);
                        Image image = new Image();
                        image.setImageUrl(imageUrl);
                        image.setProduct(product);
                        imageRepository.save(image);
                        product.getImages().add(image);
                        logger.info("Đã lưu ảnh chi tiết: {}", imageUrl);
                    } else {
                        logger.warn("Phát hiện ảnh chi tiết rỗng, bỏ qua.");
                    }
                }
            } else {
                logger.warn("Không có danh sách ảnh chi tiết được cung cấp.");
            }

            // Tạo Auction
            Auction auction = new Auction();
            auction.setProduct(product);
            auction.setAuctionStartTime(dto.getAuctionStartTime());
            auction.setAuctionEndTime(dto.getAuctionEndTime());
            auction.setBidStep(dto.getBidStep());
            auction.setCurrentPrice(dto.getBasePrice());
            logger.info("Khởi tạo đấu giá với thời gian bắt đầu: {}, thời gian kết thúc: {}", dto.getAuctionStartTime(), dto.getAuctionEndTime());
            logger.info("Đặt trạng thái đấu giá: {}", dto.getStatus());
            auction.setStatus(Auction.AuctionStatus.valueOf(dto.getStatus()));
            auctionRepository.save(auction);
            logger.info("Đã lưu đấu giá thành công cho sản phẩm ID: {}", product.getProductId());

            return product;
        } catch (IOException e) {
            logger.error("Lỗi khi upload file: {}", e.getMessage(), e);
            throw new ProductCreationException("Lỗi khi upload file: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error("Lỗi trạng thái không hợp lệ: {}", e.getMessage(), e);
            throw new ProductCreationException("Trạng thái không hợp lệ: " + dto.getStatus(), e);
        } catch (Exception e) {
            logger.error("Lỗi không xác định khi tạo sản phẩm: {}", e.getMessage(), e);
            throw new ProductCreationException("Lỗi tạo sản phẩm: " + e.getMessage(), e);
        }
    }

    // Phương thức upload file giữ nguyên
    private String uploadFile(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        return uploadResult.get("secure_url").toString();
    }
}
