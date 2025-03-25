package com.example.auction_management.service.impl;

import java.util.Collections;
import com.example.auction_management.dto.AuctionInfoDto;
import com.example.auction_management.dto.ProfileResponseDTO;
import com.example.auction_management.dto.RegisteredAuctionDTO;
import com.example.auction_management.dto.ReviewDTO;
import com.example.auction_management.exception.AuctionNotFoundException;
import com.example.auction_management.exception.ResourceNotFoundException;
import com.example.auction_management.exception.UnauthorizedActionException;
import com.example.auction_management.model.*;
import com.example.auction_management.repository.*;
import com.example.auction_management.service.IAuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService implements IAuctionService {

    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final BidRepository bidRepository;
    private final CustomerRepository customerRepository;
    private final AuctionRegistrationRepository auctionRegistrationRepository;
    private final ReviewRepository reviewRepository;

    // ------------------ CORE SERVICES ----------------------

    @Override
    public List<Auction> findAll() {
        List<Auction> auctions = auctionRepository.findAllByIsDeletedFalse();
        // Log thông tin các auction và sản phẩm, bao gồm cả danh sách images
        auctions.forEach(auction -> {
            Product product = auction.getProduct();
            if (product != null) {
                log.info("Auction ID {}: Product ID {} - Name: {}",
                        auction.getAuctionId(), product.getProductId(), product.getName());
                if (product.getImages() != null) {
                    log.info("Product ID {} có {} ảnh.",
                            product.getProductId(), product.getImages().size());
                    product.getImages().forEach(image ->
                            log.info("   Image ID: {}, URL: {}", image.getId(), image.getImageUrl())
                    );
                } else {
                    log.info("Product ID {}: images là null.", product.getProductId());
                }
            } else {
                log.info("Auction ID {} không có product.", auction.getAuctionId());
            }
        });
        return auctions;
    }

    @Override
    public Optional<Auction> findById(Integer auctionId) {
        Optional<Auction> auctionOpt = auctionRepository.findByAuctionIdAndIsDeletedFalse(auctionId);
        auctionOpt.ifPresent(auction -> {
            Product product = auction.getProduct();
            if (product != null) {
                log.info("findById: Auction ID {} - Product ID {} - Name: {}",
                        auction.getAuctionId(), product.getProductId(), product.getName());
                if (product.getImages() != null) {
                    log.info("Product ID {} có {} ảnh.",
                            product.getProductId(), product.getImages().size());
                } else {
                    log.info("Product ID {}: images là null.", product.getProductId());
                }
            } else {
                log.info("findById: Auction ID {} không có product.", auction.getAuctionId());
            }
        });
        return auctionOpt;
    }

    @Override
    public Auction save(Auction auction) {
        return auctionRepository.save(auction);
    }

    @Override
    public void deleteById(Integer id) {
        Auction auction = getAuctionByIdAndCheckOwner(id);
        softDeleteAuctionAndProduct(auction);
    }

    @Override
    public List<Auction> findByStatus(Auction.AuctionStatus status) {
        return auctionRepository.findByStatus(status);
    }

    // Sửa kiểu trả về thành Optional nếu mỗi sản phẩm chỉ có 1 phiên đấu giá
    @Override
    public Optional<Auction> findByProduct(Product product) {
        return auctionRepository.findByProductAndIsDeletedFalse(product);
    }

    @Override
    public List<Auction> findOngoingAuctions() {
        return auctionRepository.findOngoingAuctions(LocalDateTime.now());
    }

    // ------------------ EXTENDED FUNCTION ----------------------

    /**
     * Kết thúc phiên đấu giá theo quyền sở hữu
     */
    @Transactional
    public void endAuction(Integer auctionId) {
        Auction auction = getAuctionByIdAndCheckOwner(auctionId);

        if (auction.getStatus().equals(Auction.AuctionStatus.ended)) {
            throw new IllegalStateException("Phiên đấu giá đã kết thúc.");
        }

        auction.setStatus(Auction.AuctionStatus.ended);
        auctionRepository.save(auction);
    }

    /**
     * Xác nhận người chiến thắng phiên đấu giá
     */
    @Transactional
    public void confirmWinner(Integer auctionId, Integer accountId) {
        Auction auction = getAuctionByIdAndCheckOwner(auctionId);

        boolean isBidValid = auction.getBids().stream()
                .anyMatch(bid -> bid.getAccount().getAccountId().equals(accountId));

        if (!isBidValid) {
            throw new UnauthorizedActionException("Người chiến thắng không hợp lệ với phiên đấu giá này.");
        }

        // Reset tất cả bid về false
        auction.getBids().forEach(bid -> bid.setIsWinner(false));

        // Gán người thắng
        auction.getBids().stream()
                .filter(bid -> bid.getAccount().getAccountId().equals(accountId))
                .forEach(bid -> bid.setIsWinner(true));

        bidRepository.saveAll(auction.getBids());
    }

    /**
     * Tìm các phiên đấu giá theo sản phẩm mà người dùng hiện tại sở hữu
     */
    public List<Auction> findMyAuctions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return auctionRepository.findByProduct_Account_Username(username);
    }

    /**
     * Lấy lịch sử đấu giá của 1 phiên đấu giá
     */
    public List<Bid> getAuctionHistory(Integer auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Không tìm thấy phiên đấu giá!"));

        return auction.getBids();
    }

    // ------------------ PRIVATE SUPPORT METHODS ----------------------

    /**
     * Lấy phiên đấu giá và kiểm tra quyền sở hữu
     */
    private Auction getAuctionByIdAndCheckOwner(Integer auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Không tìm thấy phiên đấu giá với ID: " + auctionId));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if (!auction.getProduct().getAccount().getAccountId().equals(account.getAccountId())) {
            throw new UnauthorizedActionException("Bạn không có quyền xóa auction này");
        }
        return auction;

    }
    @Transactional
    public void updateAuctionStatuses() {
        auctionRepository.updateAuctionStatuses(LocalDateTime.now());
    }
    public List<Auction> findAllWithUpdatedStatus() {
        List<Auction> auctions = auctionRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : auctions) {
            if (auction.getAuctionStartTime() == null || auction.getAuctionEndTime() == null) {
                System.out.println("Lỗi: Phiên đấu giá ID " + auction.getAuctionId() + " có thời gian null.");
                continue; // Bỏ qua nếu dữ liệu không hợp lệ
            }

            if (now.isBefore(auction.getAuctionStartTime())) {
                auction.setStatus(Auction.AuctionStatus.pending);
            } else if (now.isAfter(auction.getAuctionStartTime()) && now.isBefore(auction.getAuctionEndTime())) {
                auction.setStatus(Auction.AuctionStatus.active);
            } else {
                auction.setStatus(Auction.AuctionStatus.ended);
            }
        }

        return auctionRepository.saveAll(auctions);
    }

    @Transactional
    public void registerCustomerForAuction(Integer customerId, Integer auctionId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng không tồn tại"));
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiên đấu giá không tồn tại"));

        // Kiểm tra trạng thái phiên đấu giá
        if (auction.getStatus() != Auction.AuctionStatus.pending) {
            throw new IllegalStateException("Chỉ có thể đăng ký khi phiên ở trạng thái pending");
        }

        // Kiểm tra đã đăng ký chưa
        boolean isRegistered = auctionRegistrationRepository.existsByAuctionAndCustomer(auction, customer);
        if (isRegistered) {
            throw new IllegalStateException("Bạn đã đăng ký phiên đấu giá này");
        }

        // Lưu đăng ký
        AuctionRegistration registration = new AuctionRegistration();
        registration.setAuction(auction);
        registration.setCustomer(customer);
        auctionRegistrationRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public Page<RegisteredAuctionDTO> getRegisteredAuctionsByCustomerId(Integer customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("auction.auctionStartTime").descending());
        Page<AuctionRegistration> registrations = auctionRegistrationRepository.findByCustomerIdWithBids(customerId, pageable);

        return registrations.map(registration -> {
            Auction auction = registration.getAuction();
            Product product = auction.getProduct();

            RegisteredAuctionDTO dto = new RegisteredAuctionDTO();
            dto.setAuctionId(auction.getAuctionId());
            dto.setProductName(product != null ? product.getName() : "N/A");
            dto.setProductDescription(product != null ? product.getDescription() : "N/A");
            dto.setBasePrice(product != null ? product.getBasePrice() : BigDecimal.ZERO);
            dto.setAuctionStartTime(auction.getAuctionStartTime());
            dto.setAuctionEndTime(auction.getAuctionEndTime());
            dto.setStatus(auction.getStatus());

            return dto;
        });
    }

    @Transactional
    public void unregisterCustomerFromAuction(Integer customerId, Integer auctionId) {
        AuctionRegistration registration = auctionRegistrationRepository
                .findByCustomer_CustomerIdAndAuction_AuctionId(customerId, auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa đăng ký phiên đấu giá này"));

        Auction auction = registration.getAuction();

        if (auction.getStatus() != Auction.AuctionStatus.pending) {
            throw new IllegalStateException("Chỉ có thể hủy đăng ký khi phiên ở trạng thái pending");
        }

        auctionRegistrationRepository.delete(registration);
        // Kiểm tra xem auction còn đăng ký nào không
        List<AuctionRegistration> remainingRegistrations =
                auctionRegistrationRepository.findByAuction_AuctionId(auctionId);

        // Nếu không còn đăng ký nào, xóa mềm auction và product
        if (remainingRegistrations.isEmpty()) {
            softDeleteAuctionAndProduct(auction);
        }
    }

    private void softDeleteAuctionAndProduct(Auction auction) {
        // Xóa mềm auction
        auction.setIsDeleted(true);
        auctionRepository.save(auction);

        // Xóa mềm product liên quan
        Product product = auction.getProduct();
        product.setIsDeleted(true);
        productRepository.save(product);
    }

    @Override
    public Map<Integer, Long> countAuctionsByMonth() {
        List<Object[]> results = auctionRepository.countAuctionsByMonth();
        Map<Integer, Long> auctionCounts = new LinkedHashMap<>();

        // Khởi tạo danh sách đủ 12 tháng, mặc định giá trị là 0
        for (int i = 1; i <= 12; i++) {
            auctionCounts.put(i, 0L);
        }

        // Gán giá trị từ query vào map
        for (Object[] result : results) {
            Integer month = (Integer) result[0];  // Tháng
            Long count = ((Number) result[1]).longValue();  // Số lượng đấu giá
            auctionCounts.put(month, count);
        }

        return auctionCounts;
    }

    public ProfileResponseDTO getUserProfile(Integer accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Customer customer = account.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Customer not found");
        }

        ProfileResponseDTO profile = new ProfileResponseDTO();

        // Lấy thông tin avatar
        String avatarUrl = customer.getAvatar() != null ? customer.getAvatar().getImageUrl() : null;

        // Lấy danh sách auction
        List<Auction> auctions = auctionRepository.findActiveAuctionsByAccountId(accountId);

        // Convert sang DTO
        List<AuctionInfoDto> auctionDtos = auctions.stream()
                .map(auction -> {
                    AuctionInfoDto dto = new AuctionInfoDto();
                    dto.setProductName(auction.getProduct().getName());
                    dto.setAuctionStartTime(auction.getAuctionStartTime());
                    dto.setAuctionEndTime(auction.getAuctionEndTime());
                    dto.setBasePrice(auction.getProduct().getBasePrice());
                    dto.setHighestBid(auction.getHighestBid());
                    dto.setStatus(auction.getStatus());
                    return dto;
                })
                .toList();

        profile.setAvatarUrl(avatarUrl);
        profile.setUsername(account.getUsername());
        profile.setFullName(customer.getName());
        profile.setAuctions(auctionDtos);

        // Lấy đánh giá
        List<Review> reviews = reviewRepository.findBySeller_CustomerId(customer.getCustomerId());
        List<ReviewDTO> reviewDTOs = reviews.stream()
                .map(this::convertToReviewDTO)
                .toList();

        profile.setAverageRating(customer.getAverageRating());
        profile.setReviews(reviewDTOs);
        return profile;
    }

    private ReviewDTO convertToReviewDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setBuyerName(review.getBuyer().getName());
        dto.setProductName(review.getBid().getAuction().getProduct().getName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}