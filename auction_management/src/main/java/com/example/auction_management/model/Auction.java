package com.example.auction_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "auctions")
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_id", columnDefinition = "INT")
    private Long auctionId;

    @NotNull(message = "Sản phẩm không được để trống")
    @ManyToOne
    @JoinColumn(name = "product_id", columnDefinition = "INT NOT NULL")
    private Product product;

    @Future(message = "Thời gian bắt đầu phải trong tương lai")
    @Column(name = "auction_start_time", columnDefinition = "DATETIME NOT NULL")
    private LocalDateTime auctionStartTime;

    @Future(message = "Thời gian kết thúc phải trong tương lai")
    @Column(name = "auction_end_time", columnDefinition = "DATETIME NOT NULL")
    private LocalDateTime auctionEndTime;

    @DecimalMin(value = "0.00", message = "Giá hiện tại không được âm")
    @Column(name = "current_price", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal currentPrice;

    @DecimalMin(value = "0.01", message = "Bước giá phải lớn hơn 0")
    @Column(name = "bid_step", columnDefinition = "DECIMAL(10,2) NOT NULL")
    private BigDecimal bidStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('pending','active','completed','canceled') DEFAULT 'pending'")
    private AuctionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    public enum AuctionStatus {
        pending, active, completed, canceled
    }
}
