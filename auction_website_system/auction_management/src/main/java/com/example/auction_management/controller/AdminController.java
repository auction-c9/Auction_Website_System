package com.example.auction_management.controller;

import com.example.auction_management.dto.TransactionDTO;
import com.example.auction_management.model.Account;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Product;
import com.example.auction_management.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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

    @Autowired
    private ITransactionService transactionService;

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

    @GetMapping("/products/{productId}")
    public ResponseEntity<Product> getProductById(@PathVariable Integer productId) {
        Product product = productService.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable Integer productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok("Sản phẩm và đấu giá liên quan đã được xóa thành công!");
    }

    @GetMapping("/products/all")
    public ResponseEntity<Page<Product>> getAllProductsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getAllProducts(pageable); // Lấy tất cả sản phẩm

        return new ResponseEntity<>(products, HttpStatus.OK);
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

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionDTO>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) { // Thêm phân trang
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionDTO> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transaction-statistics")
    public ResponseEntity<List<Map<String, Object>>> getTransactionStatistics(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(transactionService.getTotalTransactionsByDay(days));
    }



}