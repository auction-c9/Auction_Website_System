package com.example.auction_management.controller;

import com.example.auction_management.dto.TransactionDTO;
import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Transaction;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.TransactionRepository;
import com.example.auction_management.service.PaypalService;
import com.example.auction_management.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:3000")
public class TransactionController {

    @Autowired
    private VnpayService vnPayService;

    @Autowired
    private PaypalService payPalService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    // Tạo giao dịch mới và chuyển hướng đến trang thanh toán
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createTransaction(@RequestBody TransactionDTO dto) {
        Optional<Customer> customerOpt = customerRepository.findById(dto.getCustomerId().intValue());
        Optional<Auction> auctionOpt = auctionRepository.findById(dto.getAuctionId().intValue());

        if (customerOpt.isEmpty() || auctionOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Customer hoặc Auction không tồn tại!"));
        }

        Customer customer = customerOpt.get();
        Auction auction = auctionOpt.get();
        String redirectUrl = "";

        if ("VNPAY".equalsIgnoreCase(dto.getPaymentMethod())) {
            // Gọi service VNPAY và để service tự tạo transaction
            try {
                redirectUrl = vnPayService.createPaymentUrl(
                        customer.getCustomerId(),
                        auction.getAuctionId(),
                        dto.getAmount(),
                        dto.getReturnUrl()
                );
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
            }
        } else if ("PAYPAL".equalsIgnoreCase(dto.getPaymentMethod())) {
            // Logic cho PayPal (giữ nguyên nếu service PayPal chưa xử lý transaction)
            String generatedTxnId = payPalService.generateTransactionId();
            redirectUrl = payPalService.createPayment(
                    customer.getCustomerId(),
                    auction.getAuctionId(),
                    dto.getAmount(),
                    generatedTxnId,
                    "http://localhost:8080/api/transactions/paypal-return?transactionId="
                            + generatedTxnId + "&returnUrl=" + URLEncoder.encode(dto.getReturnUrl(), StandardCharsets.UTF_8)
            );

            Transaction transaction = new Transaction();
            transaction.setCustomer(customer);
            transaction.setAuction(auction);
            transaction.setAmount(dto.getAmount());
            transaction.setTransactionType(dto.getTransactionType());
            transaction.setPaymentMethod(dto.getPaymentMethod());
            transaction.setStatus("PENDING");
            transaction.setTransactionId(generatedTxnId);
            transaction.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
        } else {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Phương thức thanh toán không hợp lệ!"));
        }

        return ResponseEntity.ok(Collections.singletonMap("redirectUrl", redirectUrl));
    }

    // Xử lý phản hồi từ VNPay
    @GetMapping("/vnpay-return")
    public void returnPayment(
            @RequestParam("vnp_ResponseCode") String responseCode,
            @RequestParam("vnp_TxnRef") String vnpTxnRef,
            @RequestParam("returnUrl") String returnUrl,
            HttpServletResponse response) throws IOException {
        ResponseEntity<String> resultEntity = vnPayService.handleVnPayReturn(responseCode, vnpTxnRef);
        // Giả sử nếu vnp_ResponseCode = "00" thì thanh toán thành công, ngược lại thất bại
        String status = "00".equals(responseCode) ? "SUCCESS" : "FAILED";

        response.sendRedirect(returnUrl + "?status=" + status);
    }

    // Xử lý phản hồi từ PayPal
    @GetMapping("/paypal-return")
    public void paypalReturn(
            @RequestParam("transactionId") String transactionId,
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId,
            @RequestParam("returnUrl") String returnUrl,
            HttpServletResponse response) throws IOException {
        String result = payPalService.handlePayPalReturn(transactionId, paymentId, payerId);
        String status = result.contains("SUCCESS") ? "SUCCESS" : "FAILED";
        String finalUrl = UriComponentsBuilder.fromHttpUrl(returnUrl)
                .queryParam("status", status)
                .build()
                .toUriString();

        response.sendRedirect(finalUrl);
    }
}
