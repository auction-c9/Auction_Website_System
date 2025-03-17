package com.example.auction_management.repository;

import com.example.auction_management.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token,Integer> {
    Optional<Token> findByCode(String token);
    Optional<Token> findByCodeAndTokenType(String code, Token.TokenType tokenType);
    Optional<Token> findByCodeAndIdentifierAndTokenType(String code, String identifier, Token.TokenType tokenType);
    void deleteByIdentifierAndTokenType(String identifier, Token.TokenType tokenType);
}

