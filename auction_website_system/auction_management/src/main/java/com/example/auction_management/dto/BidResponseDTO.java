package com.example.auction_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
public class BidResponseDTO {
    private Integer bidId;
    private Integer auctionId;
    private Integer customerId;
    private BigDecimal bidAmount;
    private LocalDateTime bidTime;
    private Boolean isWinner;
    private String message;
}
