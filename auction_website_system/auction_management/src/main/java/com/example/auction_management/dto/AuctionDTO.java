package com.example.auction_management.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AuctionDTO {
    private Integer auctionId;
    private String productName;
    private BigDecimal highestBid;
    private LocalDateTime auctionEndTime;
}
