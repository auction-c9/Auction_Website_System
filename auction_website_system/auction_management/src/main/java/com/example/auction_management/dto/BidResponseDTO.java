package com.example.auction_management.dto;

import com.example.auction_management.model.Transaction;
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
public class BidResponseDTO {
    private Integer bidId;
    private Integer auctionId;
    private Integer customerId;
    private BigDecimal bidAmount;
    private LocalDateTime bidTime;
    private Boolean isWinner;

}
