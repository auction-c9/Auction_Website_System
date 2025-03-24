package com.example.auction_management.service;

import com.example.auction_management.config.VnpayConfig;
import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Transaction;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class VnpayService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    public String createPaymentUrl(Integer customerId, Integer auctionId, Double amount, String returnUrl) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }
        String vnpTxnRef = UUID.randomUUID().toString(); // Mã giao dịch VNPay (Duy nhất)
        String transactionId = UUID.randomUUID().toString();

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        long amountForVNPay = Math.round(amount * 100); // VNPay yêu cầu số tiền nhân 100

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // Lưu giao dịch với trạng thái PENDING và set vnpTxnRef
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId); // ID nội bộ
        transaction.setVnpTxnRef(vnpTxnRef); // Mã giao dịch VNPay
        transaction.setCustomer(customer);
        transaction.setAuction(auction);
        transaction.setAmount(amount);
        transaction.setTransactionType("DEPOSIT");
        transaction.setPaymentMethod("VNPAY");
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        String vnp_TmnCode = VnpayConfig.vnp_TmnCode;
        String vnp_IpAddr = "127.0.0.1";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountForVNPay));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnpTxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toán đặt cọc tài khoản: " + customerId);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        String callbackUrl = "http://localhost:8080/api/transactions/vnpay-return?returnUrl="
                + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8);
        vnp_Params.put("vnp_ReturnUrl", callbackUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII)).append('&');
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII)).append('&');
            }
        }
        if (query.length() > 0) {
            query.setLength(query.length() - 1);
        }
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }

        String vnp_SecureHash = VnpayConfig.hmacSHA512(VnpayConfig.secretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);

        return VnpayConfig.vnp_PayUrl + "?" + query;
    }

    public ResponseEntity<String> handleVnPayReturn(String responseCode, String vnp_TxnRef) {
        if("00".equals(responseCode)) {
            Transaction transaction = transactionRepository.findByVnpTxnRef(vnp_TxnRef);
            if (transaction != null) {
                transaction.setStatus("SUCCESS");
                // Nếu thanh toán thành công và transactionType đang là "DEPOSIT", thì đổi thành "FINAL"
                if ("DEPOSIT".equals(transaction.getTransactionType())) {
                    transaction.setTransactionType("FINAL");
                }
                transactionRepository.save(transaction);
                return ResponseEntity.ok("Thanh toán thành công!");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Giao dịch không tồn tại!");
            }
        } else {
            Transaction transaction = transactionRepository.findByVnpTxnRef(vnp_TxnRef);
            if (transaction != null) {
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thanh toán thất bại! Mã lỗi: " + responseCode);
        }
    }
}
