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
    private Integer accountId;

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

    @ManyToOne
    @JoinColumn(name = "id_role", nullable = false)
    private Role role;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
    private Customer customer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", columnDefinition = "ENUM('LOCAL', 'GOOGLE') DEFAULT 'LOCAL'")
    private AuthProvider authProvider = AuthProvider.LOCAL;

    public enum AccountStatus {
        active, inactive
    }

    public enum AuthProvider {
        LOCAL,
        GOOGLE
    }
}
