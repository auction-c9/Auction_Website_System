package com.example.auction_management.controller;

import com.example.auction_management.dto.AuctionDTO;
import com.example.auction_management.mapper.AuctionMapper;
import com.example.auction_management.dto.RegisteredAuctionDTO;
import com.example.auction_management.exception.ResourceNotFoundException;
import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Auction.AuctionStatus;
import com.example.auction_management.model.Product;
import com.example.auction_management.service.impl.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;


import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AuctionController {

    private final AuctionService auctionService;
    private final AuctionMapper auctionMapper;

    public AuctionController(AuctionService auctionService, AuctionMapper auctionMapper) {
        this.auctionService = auctionService;
        this.auctionMapper = auctionMapper;
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
    public ResponseEntity<List<RegisteredAuctionDTO>> getRegisteredAuctions(Authentication authentication) {
        try {
            // Kiểm tra authentication hợp lệ
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Lấy customerId từ details
            Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
            Integer customerId = (Integer) details.get("customerId");

            List<RegisteredAuctionDTO> auctions = auctionService.getRegisteredAuctionsByCustomerId(customerId);
            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/cancel/{auctionId}")
    public ResponseEntity<?> cancelAuction(@PathVariable Integer auctionId, Authentication authentication) {
        try {
            Integer customerId = (Integer) ((Map<?, ?>) authentication.getDetails()).get("customerId");
            auctionService.cancelAuction(auctionId, customerId);
            return ResponseEntity.ok(Map.of("message", "Hủy phiên đấu giá thành công"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi hệ thống"));
        }
    }
}