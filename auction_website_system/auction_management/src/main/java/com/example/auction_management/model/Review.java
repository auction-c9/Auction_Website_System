package com.example.auction_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "seller_id", referencedColumnName = "customer_id", nullable = false)
    private Customer seller;

    @ManyToOne
    @JoinColumn(name = "buyer_id", referencedColumnName = "customer_id", nullable = false)
    private Customer buyer;

    @ManyToOne
    @JoinColumn(name = "bid_id", nullable = false)
    private Bid bid;

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}