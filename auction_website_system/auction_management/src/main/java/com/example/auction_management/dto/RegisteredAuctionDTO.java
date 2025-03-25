package com.example.auction_management.dto;

import com.example.auction_management.model.Auction;
import lombok.*;

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
}
