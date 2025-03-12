package com.example.auction_management.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.auction_management.config.CloudinaryConfig;
import com.example.auction_management.dto.ProductDTO;
import com.example.auction_management.exception.ProductCreationException;
import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Category;
import com.example.auction_management.model.Image;
import com.example.auction_management.model.Product;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.repository.CategoryRepository;
import com.example.auction_management.repository.ImageRepository;
import com.example.auction_management.repository.ProductRepository;
import com.example.auction_management.service.IProductService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    // Tạo sản phẩm đấu giá mới từ ProductDTO với quản lý giao dịch và xử lý rollback
    @Transactional
    public Product createProduct(ProductDTO dto) {
        try {
            logger.info("Bắt đầu tạo sản phẩm với tên: {}", dto.getName());
            // Kiểm tra và lấy Category
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ProductCreationException("Không tìm thấy danh mục với id: " + dto.getCategoryId()));

            // Tạo Product
            Product product = new Product();
            product.setName(dto.getName());
            product.setCategory(category);
            product.setDescription(dto.getDescription());
            product.setBasePrice(dto.getBasePrice());
            product.setIsDeleted(false);

            // Upload ảnh đại diện
            if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
                String mainImageUrl = uploadFile(dto.getImageFile());
                product.setImage(mainImageUrl);
                logger.info("Upload ảnh đại diện thành công.");
            }

            // Lưu Product trước để lấy ID
            product = productRepository.save(product);

            // Khởi tạo danh sách ảnh nếu chưa có
            // Khởi tạo danh sách ảnh nếu chưa có
            if (product.getImages() == null) {
                product.setImages(new ArrayList<>());
            }

            // Upload danh sách ảnh chi tiết
            if (dto.getImageFiles() != null && !dto.getImageFiles().isEmpty()) {
                for (MultipartFile file : dto.getImageFiles()) {
                    if (!file.isEmpty()) {
                        String imageUrl = uploadFile(file);
                        Image image = new Image();
                        image.setImageUrl(imageUrl);
                        image.setProduct(product);
                        imageRepository.save(image);
                        // Thêm ảnh vào danh sách của Product
                        product.getImages().add(image);
                    }
                }
                logger.info("Upload ảnh chi tiết thành công.");
            }

            // Tạo Auction
            Auction auction = new Auction();
            auction.setProduct(product);
            auction.setAuctionStartTime(dto.getAuctionStartTime());
            auction.setAuctionEndTime(dto.getAuctionEndTime());
            auction.setBidStep(dto.getBidStep());
            auction.setCurrentPrice(dto.getBasePrice()); // Giá hiện tại ban đầu bằng giá khởi điểm
            auction.setStatus(Auction.AuctionStatus.valueOf(dto.getStatus().toLowerCase()));
            auctionRepository.save(auction);
            logger.info("Tạo phiên đấu giá thành công cho sản phẩm: {}", product.getProductId());

            return product;
        } catch (IOException e) {
            logger.error("Lỗi upload ảnh: {}", e.getMessage());
            throw new ProductCreationException("Lỗi khi upload ảnh lên Cloudinary: " + e.getMessage(), e);
        }
    }

    // Tách riêng logic upload file
    private String uploadFile(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        return uploadResult.get("secure_url").toString();
    }
}
