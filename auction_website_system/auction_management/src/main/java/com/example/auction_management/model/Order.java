package com.example.auction_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", columnDefinition = "INT")
    private Integer orderId;

    @ManyToOne
    @JoinColumn(name = "customer_id", columnDefinition = "INT")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "auction_id", columnDefinition = "INT")
    private Auction auction;

    @Future(message = "Hạn thanh toán phải trả")
    @Column(name = "payment_deadline", columnDefinition = "DATETIME")
    private LocalDateTime paymentDeadline;

    @Column(name = "shipping_address", columnDefinition = "VARCHAR(255)")
    private String shippingAddress;

    @Column(name = "full_name", columnDefinition = "VARCHAR(255)")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('pending','paid','shipped','completed','canceled') DEFAULT 'pending'")
    private OrderStatus status;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        pending, paid, shipped, completed, canceled
    }
}
