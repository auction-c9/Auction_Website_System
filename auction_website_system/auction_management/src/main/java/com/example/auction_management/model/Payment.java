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
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", columnDefinition = "INT")
    private Integer paymentId;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", columnDefinition = "ENUM('PAYPAL','VNPAY') DEFAULT 'PAYPAL'")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Loại thanh toán không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", columnDefinition = "ENUM('DEPOSIT','FINAL') DEFAULT 'DEPOSIT'")
    private PaymentType paymentType;

    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    @Column(name = "amount", columnDefinition = "DECIMAL(10,2) NOT NULL")
    private BigDecimal amount;

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "shipping_method")
    private String shippingMethod;

    @CreationTimestamp
    @Column(name = "deposit_time", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime depositTime;

    @Column(name = "payment_date", columnDefinition = "DATETIME NULL")
    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('PENDING','SUCCESS','FAILED') DEFAULT 'PENDING'")
    private PaymentStatus status;

    public enum PaymentMethod {
        PAYPAL, VNPAY
    }

    public enum PaymentType {
        DEPOSIT, FINAL
    }

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED
    }
}
