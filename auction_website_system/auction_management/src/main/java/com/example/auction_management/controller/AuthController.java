package com.example.auction_management.controller;

import com.example.auction_management.dto.LoginRequest;
import com.example.auction_management.model.Account;
import com.example.auction_management.service.IAccountService;
import com.example.auction_management.service.impl.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Autowired
    public IAccountService accountService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @GetMapping("/check-status")
    public ResponseEntity<?> checkAccountStatus(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập");
        }

        Optional<Account> optionalAccount = accountService.findAccountByUsername(authentication.getName());
        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Tài khoản không tồn tại");
        }

        Account account = optionalAccount.get();
        if ("inactive".equals(account.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tài khoản bị khóa");
        }

        return ResponseEntity.ok("Tài khoản hợp lệ");
    }
}

