package com.example.auction_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BidUpdateDTO {
    private Integer auctionId;
    private BigDecimal currentPrice;
    private Integer customerId;
}
