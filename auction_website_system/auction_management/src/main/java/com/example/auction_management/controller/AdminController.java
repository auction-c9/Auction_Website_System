package com.example.auction_management.controller;

import com.example.auction_management.dto.StatisticsDTO;
import com.example.auction_management.model.Account;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Product;
import com.example.auction_management.model.Transaction;
import com.example.auction_management.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private IProductService productService;

    @Autowired
    private IAuctionService auctionService;

    @GetMapping("/profile")
    public ResponseEntity<Account> getAdminProfile(Principal principal) {
        String username = principal.getName();
        Account account = accountService.findAccountByUsername(username).orElseThrow(() -> new RuntimeException("Account not found"));
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @GetMapping("/customers")
    public ResponseEntity<Page<Customer>> getAllCustomers(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "5") int size) {

        Page<Customer> customers = customerService.getCustomers(page, size);
        return new ResponseEntity<>(customers, HttpStatus.OK);
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Integer customerId) {
        Customer customer = customerService.findById(customerId).orElseThrow(() -> new RuntimeException("Customer not found"));
        return new ResponseEntity<>(customer, HttpStatus.OK);

    }

    @GetMapping("/products")
    public ResponseEntity<Page<Product>> getAllProducts(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "5") int size){
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getProducts(pageable);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<Product> getProductById(@PathVariable Integer productId) {
        Product product = productService.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @GetMapping("/user-statistics")
    public ResponseEntity<List<Map<String, Object>>> getNewUsers(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(accountService.getNewUsersByDay(days));
    }

    @GetMapping("/auction-statistics")
    public ResponseEntity<Map<Integer, Long>> getAuctionStatistics() {
        Map<Integer, Long> auctionStats = auctionService.countAuctionsByMonth();
        return ResponseEntity.ok(auctionStats);
    }


}