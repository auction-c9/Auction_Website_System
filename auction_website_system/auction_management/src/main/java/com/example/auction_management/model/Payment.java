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
    private Long paymentId;

    @ManyToOne
    @JoinColumn(name = "customer_id", columnDefinition = "INT")
    private Customer customer;

    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    @Column(name = "amount", columnDefinition = "DECIMAL(10,2) NOT NULL")
    private BigDecimal amount;

    @CreationTimestamp
    @Column(name = "deposit_time", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime depositTime;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "method", columnDefinition = "ENUM('credit_card','paypal','bank_transfer') DEFAULT 'credit_card'")
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('pending','completed','failed') DEFAULT 'pending'")
    private PaymentStatus status;

    public enum PaymentMethod {
        credit_card, paypal, bank_transfer
    }

    public enum PaymentStatus {
        pending, completed, failed
    }

}
