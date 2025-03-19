package com.example.auction_management.controller;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Customer;
import com.example.auction_management.service.IAccountService;
import com.example.auction_management.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin("*")
public class AdminController {
    //    @GetMapping("/users")
//    public ResponseEntity<?> getAllUsers() {
//        // Chỉ ADMIN mới được truy cập
//    }
    @Autowired
    private ICustomerService customerService;

    @Autowired
    private IAccountService accountService;

    @GetMapping("/profile")
    public ResponseEntity<Account> getAdminProfile(Principal principal) {
        String username = principal.getName();
        Account account = accountService.findAccountByUsername(username).orElseThrow(() -> new RuntimeException("Account not found"));
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @GetMapping("/customers")
    public ResponseEntity<Page<Customer>> getAllCustomers(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Customer> customers = customerService.findAll(pageable);
        return new ResponseEntity<>(customers, HttpStatus.OK);
    }
}
