package com.example.auction_management.controller;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Auction.AuctionStatus;
import com.example.auction_management.model.Product;
import com.example.auction_management.service.impl.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuctionController {

    private final AuctionService auctionService;

    @GetMapping
    public ResponseEntity<List<Auction>> getAllAuctions() {
        return ResponseEntity.ok(auctionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAuctionById(@PathVariable Integer id) {
        Optional<Auction> auction = auctionService.findById(id);
        return auction.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body((Auction) Map.of("message", "Không tìm thấy phiên đấu giá")));
    }

    @PostMapping
    public ResponseEntity<Auction> createAuction(@RequestBody Auction auction) {
        Auction savedAuction = auctionService.save(auction);
        return ResponseEntity.status(201).body(savedAuction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAuction(@PathVariable Integer id) {
        auctionService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Xóa phiên đấu giá thành công"));
    }

    // ----------- Các API mở rộng ------------

    // Lấy danh sách phiên đấu giá theo trạng thái
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getAuctionsByStatus(@PathVariable String status) {
        try {
            AuctionStatus auctionStatus = AuctionStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(auctionService.findByStatus(auctionStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trạng thái không hợp lệ")); // ✅ Trả về message rõ ràng hơn
        }
    }

    // Lấy danh sách phiên đấu giá theo ID sản phẩm
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Auction>> getAuctionsByProduct(@PathVariable Integer productId) {
        Product product = new Product();
        product.setProductId(productId);
        return ResponseEntity.ok(auctionService.findByProduct(product));
    }

    // Lấy danh sách các phiên đang diễn ra
    @GetMapping("/ongoing")
    public ResponseEntity<List<Auction>> getOngoingAuctions() {
        return ResponseEntity.ok(auctionService.findOngoingAuctions());
    }
}
