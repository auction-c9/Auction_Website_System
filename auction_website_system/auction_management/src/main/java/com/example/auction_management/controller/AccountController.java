package com.example.auction_management.controller;

import com.example.auction_management.service.IAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
}