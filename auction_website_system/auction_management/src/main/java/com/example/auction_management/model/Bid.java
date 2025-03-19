package com.example.auction_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Convert;
import org.hibernate.type.NumericBooleanConverter;

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
    @Column(name = "bid_id")
    private Integer bidId;

    @NotNull(message = "Phiên đấu giá không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @NotNull(message = "Khách hàng không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull(message = "Giá đấu không được để trống")
    @DecimalMin(value = "0.01", message = "Giá đấu phải lớn hơn 0")
    @Column(name = "bid_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal bidAmount;

    @CreationTimestamp
    @Column(name = "bid_time", updatable = false)
    private LocalDateTime bidTime;



    @Column(name = "is_winner", nullable = false)
    @Convert(converter = NumericBooleanConverter.class)
    private Boolean isWinner;

    // Thêm getter để hỗ trợ ProductService khi gọi bid.getAccount()
    public Account getAccount() {
        return customer != null ? customer.getAccount() : null;
    }
}
