package com.example.auction_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id", columnDefinition = "INT")
    private Integer customerId;

    @NotBlank(message = "Tên không được để trống")
    @Column(name = "name", columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @Email(message = "Email phải đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    @Column(name = "email", columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String email;

    @Pattern(regexp = "\\d{10,15}", message = "Số điện thoại phải chứa từ 10 đến 15 chữ số")
    @Column(name = "phone", columnDefinition = "VARCHAR(50)")
    private String phone;

    @Column(name = "identity_card", columnDefinition = "VARCHAR(50)")
    private String identityCard;

    @Column(name = "address", columnDefinition = "VARCHAR(255)")
    private String address;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private Account account;

    @DecimalMin(value = "0.0", inclusive = true, message = "Số dư không được âm")
    @Column(name = "balance", columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal balance;

    @Min(value = 0, message = "Đánh giá không được nhỏ hơn 0")
    @Column(name = "rating", columnDefinition = "INT")
    private Integer rating;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "is_deleted", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDeleted;
}
