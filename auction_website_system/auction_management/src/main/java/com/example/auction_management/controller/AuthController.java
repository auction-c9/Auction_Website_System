package com.example.auction_management.controller;

import com.example.auction_management.dto.*;
import com.example.auction_management.model.Account;
import com.example.auction_management.service.IAccountService;
import com.example.auction_management.service.impl.AuthService;
import com.example.auction_management.service.impl.CaptchaService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@CrossOrigin("*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CaptchaService captchaService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthController(AuthService authService, CaptchaService captchaService, GoogleIdTokenVerifier googleIdTokenVerifier) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    @Autowired
    public IAccountService accountService;

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    @GetMapping("/profile")
    public ResponseEntity<CustomerDTO> getProfile(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(authService.getCustomerProfile(username));
    }

    // Cập nhật thông tin cá nhân
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(@ModelAttribute CustomerDTO customerDTO, Authentication authentication) {
        log.info("Received CustomerDTO: {}", customerDTO);
        String username = authentication.getName();
        return ResponseEntity.ok(authService.updateCustomerProfile(username, customerDTO));
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<?> changePassword(@Validated @RequestBody ChangePasswordRequest request, Authentication authentication) {
        authService.changePassword(authentication.getName(), request.getCurrentPassword(), request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Validated(ForgotPasswordDTO.Step3.class) @RequestBody ForgotPasswordDTO dto) {
        authService.resetPassword(dto.getUsername(), dto.getNewPassword(), dto.getConfirmPassword());
        return ResponseEntity.ok("Đặt lại mật khẩu thành công");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        System.out.println("Received login request for username: " + loginRequest.getUsername());
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

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> register(@Validated @ModelAttribute AccountDto accountDto, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        try {
            // Kiểm tra captcha
            String storedAnswer = (String) session.getAttribute("captchaAnswer");
            if (storedAnswer == null) {
                throw new RuntimeException("Không tìm thấy captcha trong session");
            }

            // Kiểm tra câu trả lời captcha
            if (!accountDto.getCaptcha().equals(storedAnswer)) {
                response.put("message", "Mã xác minh không chính xác!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            authService.register(accountDto, storedAnswer);
            session.removeAttribute("captchaAnswer");
            response.put("message", "Đăng ký thành công!");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Xử lý lỗi logic nghiệp vụ
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            // Xử lý lỗi server
            e.printStackTrace();
            response.put("message", "Lỗi hệ thống. Vui lòng thử lại sau!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Validated(ForgotPasswordDTO.Step1.class) @RequestBody ForgotPasswordDTO dto) {
        authService.initiatePasswordReset(dto.getUsername());
        return ResponseEntity.ok("Mã xác nhận đã được gửi đến email đăng ký");
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@Validated(ForgotPasswordDTO.Step2.class) @RequestBody ForgotPasswordDTO dto) {
        authService.validateResetCode(dto.getUsername(), dto.getCode());
        return ResponseEntity.ok("Mã xác nhận hợp lệ");
    }
}