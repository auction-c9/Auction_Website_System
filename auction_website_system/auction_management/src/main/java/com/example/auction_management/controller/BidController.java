package com.example.auction_management.controller;

import com.example.auction_management.dto.BidDTO;
import com.example.auction_management.dto.BidRequestDTO;
import com.example.auction_management.dto.BidResponseDTO;
import com.example.auction_management.service.impl.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BidController {

    private final BidService bidService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<List<BidResponseDTO>> getBidHistoryByCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Integer customerId = bidService.getCustomerIdFromUsername(userDetails.getUsername());
        List<BidResponseDTO> bidHistory = bidService.getBidHistoryByCustomerId(customerId);
        return ResponseEntity.ok(bidHistory);
    }


    /**
     * API đặt giá mới cho phiên đấu giá
     */
    @PostMapping("/auction/{auctionId}")
    public ResponseEntity<BidResponseDTO> placeBid(@PathVariable Integer auctionId,
                                                   @Valid @RequestBody BidRequestDTO bidRequestDTO,
                                                   Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BidResponseDTO.builder().message("Bạn cần đăng nhập để đặt giá!").build());
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Integer customerId = bidService.getCustomerIdFromUsername(userDetails.getUsername());

        // Tạo BidDTO
        BidDTO bidDTO = new BidDTO();
        bidDTO.setAuctionId(auctionId);
        bidDTO.setCustomerId(customerId);
        bidDTO.setBidAmount(bidRequestDTO.getBidAmount());

        BidResponseDTO bidResponse = bidService.placeBid(bidDTO);

        // Gửi qua websocket
        messagingTemplate.convertAndSend("/topic/auctions/" + auctionId, bidResponse);

        return ResponseEntity.ok(bidResponse);
    }

    /**
     * API lấy lịch sử đấu giá theo auctionId
     */
    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<List<BidResponseDTO>> getBidHistory(@PathVariable Integer auctionId) {
        List<BidResponseDTO> bidHistory = bidService.getBidHistoryByAuctionId(auctionId);
        return ResponseEntity.ok(bidHistory);
    }

    @GetMapping("/deposit/check")
    public ResponseEntity<?> checkDeposit(@RequestParam Integer auctionId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn cần đăng nhập để kiểm tra tiền đặt cọc!");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Integer customerId = bidService.getCustomerIdFromUsername(userDetails.getUsername());

        boolean hasDeposit = bidService.checkDeposit(customerId, auctionId);
        return ResponseEntity.ok(hasDeposit);
    }
}
