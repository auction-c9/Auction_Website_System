package com.example.auction_management.controller;

import com.example.auction_management.dto.BidDTO;
import com.example.auction_management.model.Bid;
import com.example.auction_management.service.impl.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BidController {

    private final BidService bidService;

    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody BidDTO bidDTO) {
        Bid bid = bidService.placeBid(bidDTO);
        return ResponseEntity.ok(Map.of(
                "message", "Đấu giá thành công!",
                "bid", bid
        ));
    }
}
