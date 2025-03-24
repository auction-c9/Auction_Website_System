package com.example.auction_management.controller;

import com.example.auction_management.dto.AuctionDTO;
import com.example.auction_management.dto.ProfileResponseDTO;
import com.example.auction_management.mapper.AuctionMapper;
import com.example.auction_management.dto.RegisteredAuctionDTO;
import com.example.auction_management.exception.ResourceNotFoundException;
import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Auction.AuctionStatus;
import com.example.auction_management.model.Product;
import com.example.auction_management.service.ICustomerService;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.service.impl.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import java.util.Collections;
import java.util.stream.Collectors;


import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auctions")
@CrossOrigin(origins = "http://localhost:3000")
public class AuctionController {

    private final AuctionService auctionService;
    private final ICustomerService customerService;
    private final AuctionRepository auctionRepository;

    public AuctionController(AuctionService auctionService, ICustomerService customerService , AuctionRepository auctionRepository) {
        this.auctionService = auctionService;
        this.customerService = customerService;
        this.auctionRepository = auctionRepository;
    }


    @GetMapping
    public ResponseEntity<List<Auction>> getAllAuctions() {
        try {
            List<Auction> auctions = auctionService.findAll();
            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            e.printStackTrace(); // In lỗi chi tiết ra console
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAuctionById(@PathVariable Integer id) {
        Optional<Auction> auction = auctionService.findById(id);
        if (auction.isPresent()) {
            return ResponseEntity.ok(auction.get());
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy phiên đấu giá"));
        }
    }


    @PostMapping
    public ResponseEntity<Auction> createAuction(@RequestBody Auction auction) {
        Auction savedAuction = auctionService.save(auction);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAuction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAuction(@PathVariable Integer id) {
        auctionService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Xóa phiên đấu giá thành công"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getAuctionsByStatus(@PathVariable String status) {
        try {
            AuctionStatus auctionStatus = AuctionStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(auctionService.findByStatus(auctionStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trạng thái không hợp lệ"));
        }
    }

    // Lấy danh sách phiên đấu giá theo ID sản phẩm
    @GetMapping("/product/{productId}")
    public ResponseEntity<Optional<Auction>> getAuctionsByProduct(@PathVariable Integer productId) {
        Product product = new Product();
        product.setProductId(productId);
        return ResponseEntity.ok(auctionService.findByProduct(product));
    }

    // Lấy danh sách các phiên đang diễn ra
    @GetMapping("/ongoing")
    public ResponseEntity<List<Auction>> getOngoingAuctions() {
        return ResponseEntity.ok(auctionService.findOngoingAuctions());
    }

    @GetMapping("/registered-history")
    public ResponseEntity<?> getRegisteredAuctions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Integer customerId = customerService.getCustomerIdByUsername(userDetails.getUsername());

            Page<RegisteredAuctionDTO> result = auctionService.getRegisteredAuctionsByCustomerId(customerId, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error retrieving auctions"));
        }
    }

    @DeleteMapping("/cancel/{auctionId}")
    public ResponseEntity<?> unregisterFromAuction(
            @PathVariable Integer auctionId,
            Authentication authentication
    ) {
        try {
            Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
            Integer customerId = (Integer) details.get("customerId");
            auctionService.unregisterCustomerFromAuction(customerId, auctionId);
            return ResponseEntity.ok(Map.of("message", "Hủy đăng ký thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/profile/{accountID}")
    public ResponseEntity<ProfileResponseDTO> getUserProfile(@PathVariable Integer accountID) {
        ProfileResponseDTO profile = auctionService.getUserProfile(accountID);
        return ResponseEntity.ok(profile);
    }
    @GetMapping("/search")
    public ResponseEntity<List<Auction>> searchAuctions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<Integer> categoryIds,
            @RequestParam(required = false) BigDecimal minStartingPrice) {
        List<Auction> results = auctionRepository.searchAuctions(query, categoryIds, minStartingPrice);
        return ResponseEntity.ok(results);
    }
}