package com.example.auction_management.controller;

import com.auth0.jwt.algorithms.Algorithm;
import com.example.auction_management.dto.AccountDto;
import com.example.auction_management.dto.JwtResponse;
import com.example.auction_management.dto.LoginRequest;
import com.example.auction_management.model.Account;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.VerificationToken;
import com.example.auction_management.service.IAccountService;
import com.example.auction_management.service.impl.AuthService;
import com.example.auction_management.service.impl.CaptchaService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.springframework.security.config.Elements.JWT;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CaptchaService captchaService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthController(AuthService authService,CaptchaService captchaService, GoogleIdTokenVerifier googleIdTokenVerifier) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
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
    public ResponseEntity<Map<String, String>> getRegistrationForm(HttpSession session) {
        Map<String, String> captcha = captchaService.generateSimpleMathQuestion();
        session.setAttribute("captchaAnswer", captcha.get("answer"));
        return ResponseEntity.ok(Map.of("question", captcha.get("question")));
    }

    @PostMapping(value = "/register",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> register(
            @Validated @ModelAttribute AccountDto accountDto,
            HttpSession session
    ) {
        System.out.println("Session attributes: " + session.getAttributeNames());
        // Kiểm tra captcha
        String storedAnswer = (String) session.getAttribute("captchaAnswer");
        if (storedAnswer == null) {
            throw new RuntimeException("Không tìm thấy captcha trong session");
        }

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
    @PreAuthorize("hasRole('USER')") // Kiểm tra quyền

    @PostMapping("/google")
    public ResponseEntity<?> authenticateGoogle(@RequestBody Map<String, String> request) {
        try {
            String idToken = request.get("token");
            GoogleIdToken idTokenObj = googleIdTokenVerifier.verify(idToken);

            if (idTokenObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token không hợp lệ"));
            }

            // Xử lý logic nghiệp vụ trong service
            JwtResponse jwtResponse = authService.handleGoogleLogin(idTokenObj.getPayload());
            return ResponseEntity.ok(jwtResponse);

        } catch (GeneralSecurityException | IOException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Xác thực Google thất bại"));
        }
    }

    @GetMapping("/verify-link-account")
    public ResponseEntity<?> verifyLinkAccount(@RequestParam String token) {
        authService.verifyLinkAccount(token);
        return ResponseEntity.ok("Liên kết tài khoản thành công!");
    }
}

