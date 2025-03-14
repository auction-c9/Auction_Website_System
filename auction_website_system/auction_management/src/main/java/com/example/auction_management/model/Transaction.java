package com.example.auction_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer; // Tham chiếu đến User

    @ManyToOne
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction; // Tham chiếu đến Auction

    private Double amount;
    private String paymentMethod; // "VNPAY" hoặc "PAYPAL"
    private String status; // "PENDING", "SUCCESS", "FAILED"
    private String vnpTxnRef;
    @Column(name = "transaction_id", unique = true)
    private String transactionId; // Mã giao dịch dùng để đối soát với PayPal/VNPAY



}