package com.example.auction_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bids")
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id", columnDefinition = "INT")
    private Long bidId;

    @NotNull(message = "Phiên đấu giá không được để trống")
    @ManyToOne
    @JoinColumn(name = "auction_id", columnDefinition = "INT NOT NULL")
    private Auction auction;

    @NotNull(message = "Khách hàng không được để trống")
    @ManyToOne
    @JoinColumn(name = "customer_id", columnDefinition = "INT NOT NULL")
    private Customer customer;

    @DecimalMin(value = "0.01", message = "Giá đấu phải lớn hơn 0")
    @Column(name = "bid_amount", columnDefinition = "DECIMAL(10,2) NOT NULL")
    private BigDecimal bidAmount;

    @CreationTimestamp
    @Column(name = "bid_time", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime bidTime;

    @Column(name = "is_winner", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isWinner;
}
