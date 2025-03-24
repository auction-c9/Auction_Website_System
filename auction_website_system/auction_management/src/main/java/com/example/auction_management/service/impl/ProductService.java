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
import com.example.auction_management.service.EmailService;
import com.example.auction_management.service.IProductService;
import com.example.auction_management.validation.AuctionCreateGroup;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import jakarta.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
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
    private final Validator validator;

    private final EmailService emailService;

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

    private final List<String> bannedWords = Arrays.asList(
            "chống phá", "khiêu dâm", "bạo lực", "phản động", "xâm phạm chủ quyền", "an ninh quốc gia"
    ).stream().map(this::normalizeText).toList();

    private static final Pattern DIACRITIC_PATTERN = Pattern.compile("\\p{M}");

    @Transactional
    @Override
    public Product createProduct(ProductDTO dto) {
        Account account = getAuthenticatedAccount();
        Category category = getCategory(dto.getCategoryId());

        Customer customer = customerRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new ProductCreationException("Tài khoản không liên kết với Customer"));
        if (containsBannedWords(dto.getName()) || containsBannedWords(dto.getDescription())) {
            System.out.println("Vi phạm từ cấm: " + dto.getName() + " hoặc " + dto.getDescription());
            handleViolation(account);
            throw new ProductCreationException("Nội dung sản phẩm chứa từ ngữ không hợp lệ!");
        }

        try {
            Product product = buildProduct(dto, account, category);
            product = productRepository.save(product);
            uploadDetailImages(dto.getImageFiles(), product);
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

    private boolean containsBannedWords(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String cleanedText = normalizeText(text);
        System.out.println("🔍 Nội dung gốc: " + text);
        System.out.println("✅ Nội dung sau chuẩn hóa: " + cleanedText);
        boolean contains = bannedWords.stream().anyMatch(cleanedText::contains);
        System.out.println("⚠️ Kết quả kiểm tra: " + (contains ? "Có chứa từ cấm!" : "Không có từ cấm."));

        return contains;
    }

    private String normalizeText(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = DIACRITIC_PATTERN.matcher(normalized).replaceAll("");
        return normalized.replaceAll("[^\\p{L}\\p{N}\\s]", "").replaceAll("\\s+", " ").trim().toLowerCase();
    }

    @Transactional
    public void handleViolation(Account account) {
        int newCount = account.getViolationCount() + 1;
        account.setViolationCount(newCount);

        accountRepository.save(account);
        SecurityContextHolder.getContext().setAuthentication(null);

        Customer customer = account.getCustomer();
        String email = (customer != null) ? customer.getEmail() : null;

        if (email == null) {
            throw new RuntimeException("Không tìm thấy email của tài khoản");
        }

        if (newCount >= 3) {
            account.setLocked(true);
            account.setStatus(Account.AccountStatus.inactive);

            emailService.sendEmail(email, "Thông báo khóa tài khoản",
                    "Tài khoản của bạn đã bị khóa do vi phạm nội dung quá nhiều lần.");
        } else {
            emailService.sendEmail(email, "Cảnh báo vi phạm nội dung",
                    "Bạn đã vi phạm nội dung sản phẩm. Vui lòng chỉnh sửa để tránh bị khóa tài khoản.");
        }
    }

    @Override
    public Page<Product> getProducts(Pageable pageable) {
        return productRepository.findByIsDeletedFalse(pageable); // Chỉ lấy sản phẩm chưa bị ẩn
    }

    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Transactional
    @Override
    public void deletePermanently(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        product.setIsDeleted(true);
        productRepository.save(product);
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
                .winnerNotified(false) // Thêm dòng này để đảm bảo trường không null
                .build();

        // Validate theo nhóm AuctionCreateGroup
        Set<ConstraintViolation<Auction>> violations = validator.validate(auction, AuctionCreateGroup.class);
        if (!violations.isEmpty()) {
            // Nếu có lỗi, ném ra exception hoặc xử lý theo cách bạn muốn
            throw new ConstraintViolationException(violations);
        }

        auctionRepository.save(auction);
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
