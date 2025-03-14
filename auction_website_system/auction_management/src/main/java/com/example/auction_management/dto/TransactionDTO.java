package com.example.auction_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Integer id;
    private Integer customerId;
    private Integer auctionId;
    private Double amount;
    private String transactionType;
    private String paymentMethod;
    private String status;
    private LocalDateTime createdAt = LocalDateTime.now();

}
