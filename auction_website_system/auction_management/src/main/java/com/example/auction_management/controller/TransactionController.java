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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                        dto.getAmount()
                );
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
            }
        }
        else if ("PAYPAL".equalsIgnoreCase(dto.getPaymentMethod())) {
            // Logic cho PayPal (giữ nguyên nếu service PayPal chưa xử lý transaction)
            String generatedTxnId = payPalService.generateTransactionId();
            redirectUrl = payPalService.createPayment(
                    customer.getCustomerId(),
                    auction.getAuctionId(),
                    dto.getAmount(),
                    generatedTxnId
            );

            Transaction transaction = new Transaction();
            transaction.setCustomer(customer);
            transaction.setAuction(auction);
            transaction.setAmount(dto.getAmount());
            transaction.setPaymentMethod(dto.getPaymentMethod());
            transaction.setStatus("PENDING");
            transaction.setTransactionId(generatedTxnId);
            transactionRepository.save(transaction);
        }
        else {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Phương thức thanh toán không hợp lệ!"));
        }

        return ResponseEntity.ok(Collections.singletonMap("redirectUrl", redirectUrl));
    }

    // Xử lý phản hồi từ VNPay
    @GetMapping("/vnpay-return")
    public ResponseEntity<String> returnPayment(
            @RequestParam("vnp_ResponseCode") String responseCode,
            @RequestParam("vnp_TxnRef") String vnpTxnRef) {
        return vnPayService.handleVnPayReturn(responseCode, vnpTxnRef);
    }

    // Xử lý phản hồi từ PayPal
    @GetMapping("/paypal-return")
    public ResponseEntity<String> paypalReturn(
            @RequestParam("transactionId") String transactionId,
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId) {
        String result = payPalService.handlePayPalReturn(transactionId, paymentId, payerId);
        return ResponseEntity.ok(result);
    }
}
