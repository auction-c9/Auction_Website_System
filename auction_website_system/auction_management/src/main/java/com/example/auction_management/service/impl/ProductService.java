package com.example.auction_management.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.auction_management.config.CloudinaryConfig;
import com.example.auction_management.dto.ProductDTO;
import com.example.auction_management.exception.ProductCreationException;
import com.example.auction_management.exception.ProductNotFoundException;
import com.example.auction_management.exception.UnauthorizedActionException;
import com.example.auction_management.model.*;
import com.example.auction_management.repository.*;
import com.example.auction_management.service.IProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageRepository imageRepository;
    private final AuctionRepository auctionRepository;
    private final AccountRepository accountRepository;
    private final AuctionRegistrationRepository auctionRegistrationRepository;
    private final CustomerRepository customerRepository;

    private final Cloudinary cloudinary = CloudinaryConfig.getCloudinary();

    // ------------------------- CORE SERVICES ----------------------------

    @Override
    public List<Product> findAll() {
        return productRepository.findAllByIsDeletedFalse(); //
    }

    @Override
    public Optional<Product> findById(Integer id) {
        return productRepository.findByProductIdAndIsDeletedFalse(id);
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public void deleteById(Integer id) {
        Product product = getProductByIdAndCheckOwner(id);
        product.setIsDeleted(true);
        productRepository.save(product);
        List<Auction> auctions = auctionRepository.findAllByProduct(product);
        auctions.forEach(auction -> {
            auction.setIsDeleted(true);
            auctionRepository.save(auction);
        });
    }

    // ------------------------- PRODUCT MANAGEMENT ----------------------------

    @Transactional
    @Override
    public Product createProduct(ProductDTO dto) {
        Account account = getAuthenticatedAccount();
        Category category = getCategory(dto.getCategoryId());

        Customer customer = customerRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new ProductCreationException("Tài khoản không liên kết với Customer"));

        try {
            Product product = buildProduct(dto, account, category);
            product = productRepository.save(product);
            uploadDetailImages(dto.getImageFiles(), product);

            // Tạo auction và lưu vào database
            Auction auction = createAuction(dto, product);

            if (auctionRegistrationRepository.existsByAuctionAndCustomer(auction, customer)) {
                throw new ProductCreationException("Customer đã đăng ký auction này");
            }

            AuctionRegistration registration = new AuctionRegistration();
            registration.setAuction(auction);
            registration.setCustomer(customer);
            auctionRegistrationRepository.save(registration);
            return product;
        } catch (IOException e) {
            logger.error("Lỗi khi tạo sản phẩm: {}", e.getMessage(), e);
            throw new ProductCreationException("Lỗi khi tạo sản phẩm: " + e.getMessage());
        }
    }

    @Transactional

    public Product updateProduct(Integer productId, ProductDTO dto) {
        Product product = getProductByIdAndCheckOwner(productId);
        try {
            updateProductData(dto, product);
            return productRepository.save(product);
        } catch (IOException e) {
            throw new ProductCreationException("Lỗi cập nhật sản phẩm: " + e.getMessage());
        }
    }

    @Transactional

    public void endAuction(Integer productId) {
        Product product = getProductByIdAndCheckOwner(productId);
        Auction auction = auctionRepository.findByProduct(product)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy phiên đấu giá của sản phẩm này"));
        auction.setStatus(Auction.AuctionStatus.ended);
        auctionRepository.save(auction);
    }

    @Transactional

    public void confirmWinner(Integer productId, Integer accountId) {
        Product product = getProductByIdAndCheckOwner(productId);
        Auction auction = auctionRepository.findByProduct(product)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy phiên đấu giá của sản phẩm này"));

        boolean isWinnerValid = auction.getBids().stream()
                .anyMatch(bid -> bid.getAccount().getAccountId().equals(accountId));
        if (!isWinnerValid) throw new ProductCreationException("Tài khoản không hợp lệ để trở thành người chiến thắng");

        auction.getBids().forEach(bid -> bid.setIsWinner(bid.getAccount().getAccountId().equals(accountId)));
    }

    // ------------------------- EXTENDED METHODS ----------------------------


    public List<Product> findMyProducts() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return productRepository.findByAccountUsername(username).stream()
                .filter(product -> !Boolean.TRUE.equals(product.getIsDeleted()))
                .collect(Collectors.toList());
    }


    public List<Bid> getAuctionHistory(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));
        Auction auction = auctionRepository.findByProduct(product)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy phiên đấu giá của sản phẩm"));
        return auction.getBids();
    }

    // ------------------------- PRIVATE SUPPORT METHODS ----------------------------

    private Account getAuthenticatedAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new ProductCreationException("Không tìm thấy tài khoản: " + username));
    }

    private Category getCategory(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProductCreationException("Không tìm thấy danh mục: " + categoryId));
    }

    private Product buildProduct(ProductDTO dto, Account account, Category category) throws IOException {
        return Product.builder()
                .name(dto.getName())
                .category(category)
                .description(dto.getDescription())
                .basePrice(dto.getBasePrice())
                .isDeleted(false)
                .account(account)
                .image(uploadFile(dto.getImageFile()))
                .images(new ArrayList<>())
                .build();
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

    private Auction createAuction(ProductDTO dto, Product product) {
        Auction auction = Auction.builder()
                .product(product)
                .auctionStartTime(dto.getAuctionStartTime())
                .auctionEndTime(dto.getAuctionEndTime())
                .bidStep(dto.getBidStep())
                .currentPrice(dto.getBasePrice())
                .status(Auction.AuctionStatus.valueOf(dto.getStatus()))
                .isDeleted(false)

                .build();
        return auctionRepository.save(auction);
    }

    private String uploadFile(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        return uploadResult.get("secure_url").toString();
    }

    private Product getProductByIdAndCheckOwner(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm: " + productId));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!product.getAccount().getUsername().equals(username)) {
            throw new UnauthorizedActionException("Không có quyền trên sản phẩm này");
        }
        return product;
    }

    private void updateProductData(ProductDTO dto, Product product) throws IOException {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setBasePrice(dto.getBasePrice());
        product.setCategory(getCategory(dto.getCategoryId()));
        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            product.setImage(uploadFile(dto.getImageFile()));
        }
        uploadDetailImages(dto.getImageFiles(), product);
    }
}
