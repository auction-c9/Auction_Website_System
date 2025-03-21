package com.example.auction_management.controller;

import com.example.auction_management.service.IAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private IAccountService accountService;

    @PutMapping("/lock/{accountId}")
    public ResponseEntity<?> lockAccount(@PathVariable Integer accountId) {
        accountService.lockAccount(accountId);
        return ResponseEntity.ok("Tài khoản đã bị khóa");
    }

    @PutMapping("/unlock/{accountId}")
    public ResponseEntity<?> unlockAccount(@PathVariable Integer accountId) {
        accountService.unlockAccount(accountId);
        return ResponseEntity.ok("Tài khoản đã được mở khóa");
    }

    @PostMapping("/warning/{accountId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")

    public ResponseEntity<?> sendWarningEmail(@PathVariable Integer accountId,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {
        System.out.println("Token nhận được từ request: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ hoặc không được gửi lên.");
        }

        accountService.sendWarningEmail(accountId);
        return ResponseEntity.ok("Đã gửi email cảnh cáo thành công");
    }



}