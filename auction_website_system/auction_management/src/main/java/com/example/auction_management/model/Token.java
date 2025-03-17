package com.example.auction_management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@Entity(name="token")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false) // Map đến cột "code"
    private String code;

    @Column(name = "identifier", nullable = false) // Map đến cột "identifier"
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false) // Map đến cột "token_type"
    private TokenType tokenType;

    @Column(name = "expiry_date", nullable = false) // Map đến cột "expiry_date"
    private LocalDateTime expiryDate;

    public enum TokenType {
        ACCOUNT_VERIFICATION,
        PASSWORD_RESET
    }
}
