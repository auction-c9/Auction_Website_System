package com.example.auction_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id", columnDefinition = "INT")
    private Long accountId;

    @NotBlank(message = "Username không được để trống")
    @Column(name = "username", columnDefinition = "VARCHAR(255) NOT NULL UNIQUE")
    private String username;

    @NotBlank(message = "Password không được để trống")
    @Column(name = "password", columnDefinition = "VARCHAR(255) NOT NULL")
    private String password;

    @NotNull(message = "Trạng thái không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('active', 'inactive') DEFAULT 'active'")
    private AccountStatus status;

    public enum AccountStatus {
        active, inactive
    }
}
