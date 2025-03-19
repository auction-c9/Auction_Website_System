package com.example.auction_management.controller;

import com.example.auction_management.dto.TransactionDTO;
import com.example.auction_management.service.TransactionService;
import com.example.auction_management.service.VnpayService;
import com.example.auction_management.service.PaypalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:3000")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private VnpayService vnPayService;

    @Autowired
    private PaypalService payPalService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createTransaction(@RequestBody TransactionDTO dto) {
        Map<String, String> result = transactionService.createTransaction(dto);
        if (result.containsKey("message")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/vnpay-return")
    public void returnPayment(
            @RequestParam("vnp_ResponseCode") String responseCode,
            @RequestParam("vnp_TxnRef") String vnpTxnRef,
            @RequestParam("returnUrl") String returnUrl,
            HttpServletResponse response) throws IOException {
        ResponseEntity<String> resultEntity = vnPayService.handleVnPayReturn(responseCode, vnpTxnRef);
        String status = "00".equals(responseCode) ? "SUCCESS" : "FAILED";
        response.sendRedirect(returnUrl + "?status=" + status);
    }

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