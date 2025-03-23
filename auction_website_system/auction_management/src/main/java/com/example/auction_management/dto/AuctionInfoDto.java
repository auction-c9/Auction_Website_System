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
@AllArgsConstructor
@NoArgsConstructor
public class AuctionInfoDto {
    private String productName;
    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;
    private BigDecimal basePrice;
    private BigDecimal highestBid;
    private Auction.AuctionStatus status;
}
