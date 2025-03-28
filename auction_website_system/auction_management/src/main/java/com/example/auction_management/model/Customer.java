package com.example.auction_management.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "customers")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "customerId")
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

    @Column(name = "dob", columnDefinition = "DATE")
    private LocalDate dob;

    @Column(name = "identity_card", columnDefinition = "VARCHAR(50)")
    private String identityCard;

    @Column(name = "address", columnDefinition = "VARCHAR(255)")
    private String address;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private Account account;

    @Column(name = "average_rating", columnDefinition = "DECIMAL(3,2) DEFAULT 0.00")
    private BigDecimal averageRating;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Review> receivedReviews = new ArrayList<>();

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Review> givenReviews = new ArrayList<>();

    @Column(name = "is_deleted", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDeleted;

    @NotBlank(message = "Tên ngân hàng không được để trống")
    @Column(name = "bank_name", columnDefinition = "VARCHAR(255) NOT NULL")
    private String bankName;

    @NotBlank(message = "Số tài khoản không được để trống")
    @Column(name = "bank_account", columnDefinition = "VARCHAR(50) NOT NULL")
    private String bankAccount;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "avatar_id", referencedColumnName = "id")
    private Image avatar;

    // Trong lớp Customer
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<AuctionRegistration> registrations = new ArrayList<>();
}
