package com.example.auction_management.dto;

import com.example.auction_management.model.Account; // Import Account nếu cần
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private boolean hasReviewed;

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

    public BidResponseDTO(Integer bidId, Integer auctionId, Integer customerId, BigDecimal bidAmount, LocalDateTime bidTime, Boolean isWinner, String message, Account user, LocalDateTime registrationDate, String productName,String auctionStatus) {
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.customerId = customerId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.isWinner = isWinner;
        this.message = message;
        this.registrationDate = registrationDate;
        this.productName = productName;
        this.auctionStatus = auctionStatus;
        this.user = user;
    }
}
