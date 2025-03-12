package com.example.auction_management.controller;

import com.example.auction_management.dto.AccountDto;
import com.example.auction_management.dto.LoginRequest;
import com.example.auction_management.model.Account;
import com.example.auction_management.service.IAccountService;
import com.example.auction_management.service.impl.AuthService;
import com.example.auction_management.service.impl.CaptchaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService,CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @Autowired
    public IAccountService accountService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        System.out.println("Received login request for username: " + loginRequest.getUsername());
        System.out.println("Password: " + loginRequest.getPassword());
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

    @GetMapping("/register-question")
    public ResponseEntity<Map<String, String>> getRegistrationForm() {
        return ResponseEntity.ok(captchaService.generateSimpleMathQuestion());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Validated @RequestBody AccountDto accountDto,
            HttpSession session
    ) {
        // Kiểm tra captcha
        String storedAnswer = (String) session.getAttribute("captchaAnswer");

        authService.register(accountDto, storedAnswer);
        session.removeAttribute("captchaAnswer");
        return ResponseEntity.ok("Đăng ký thành công");
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication, String storedCaptchaAnswer) {
        String username = authentication.getName();
        Optional<Account> account = accountService.findAccountByUsername(username);
        return ResponseEntity.ok(account.orElseThrow(() -> new RuntimeException("User not found")));
    }
}

