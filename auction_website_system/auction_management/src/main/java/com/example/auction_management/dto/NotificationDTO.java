package com.example.auction_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private Integer notificationId;
    private Integer customerId;
    private AuctionDTO auction; // Lấy auctionId từ Auction
    private String message;
    private LocalDateTime timestamp;
    private Boolean isRead;
}
