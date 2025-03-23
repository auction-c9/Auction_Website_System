package com.example.auction_management.dto;

import com.example.auction_management.model.Account; // Import Account nếu cần
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
    private LocalDateTime registrationDate;
    private String productName;
    private String auctionStatus;

    // THÊM TRƯỜNG USER ĐỂ HIỂN THỊ THÔNG TIN TÀI KHOẢN NGƯỜI ĐẤU GIÁ
    private Account user;

    public BidResponseDTO(Integer bidId, Integer auctionId, Integer customerId, BigDecimal bidAmount, LocalDateTime bidTime, Boolean isWinner, String message, Account user) {
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.customerId = customerId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.isWinner = isWinner;
        this.message = message;
        this.user = user;
    }
}
