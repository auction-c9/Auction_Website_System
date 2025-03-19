package com.example.auction_management.dto;

import com.example.auction_management.model.Auction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredAuctionDTO {
    private Integer auctionId;
    private String productName;
    private String productDescription;
    private BigDecimal basePrice;
    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;
    private Auction.AuctionStatus status;
    private LocalDateTime createdAt;
}
