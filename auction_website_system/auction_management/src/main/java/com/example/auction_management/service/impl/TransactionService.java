package com.example.auction_management.service.impl;

import com.example.auction_management.dto.TransactionDTO;
import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Transaction;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.TransactionRepository;
import com.example.auction_management.service.ITransactionService;
import com.example.auction_management.service.PaypalService;
import com.example.auction_management.service.VnpayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TransactionService implements ITransactionService {
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

    @Override
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    @Override
    public Optional<Transaction> findById(Integer integer) {
        return Optional.empty();
    }

    @Override
    public Transaction save(Transaction entity) {
        return null;
    }

    @Override
    public void deleteById(Integer integer) {

    }

    @Override
    public Map<String, String> createTransaction(TransactionDTO dto) {
        Optional<Customer> customerOpt = customerRepository.findById(dto.getCustomerId().intValue());
        Optional<Auction> auctionOpt = auctionRepository.findById(dto.getAuctionId().intValue());

        if (customerOpt.isEmpty() || auctionOpt.isEmpty()) {
            return Collections.singletonMap("message", "Customer hoặc Auction không tồn tại!");
        }

        Customer customer = customerOpt.get();
        Auction auction = auctionOpt.get();
        String redirectUrl = "";

        if ("VNPAY".equalsIgnoreCase(dto.getPaymentMethod())) {
            try {
                redirectUrl = vnPayService.createPaymentUrl(
                        customer.getCustomerId(),
                        auction.getAuctionId(),
                        dto.getAmount(),
                        dto.getReturnUrl()
                );
            } catch (IllegalArgumentException e) {
                return Collections.singletonMap("message", e.getMessage());
            }
        } else if ("PAYPAL".equalsIgnoreCase(dto.getPaymentMethod())) {
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
            transaction.setTransactionType(dto.getTransactionType() != null ? dto.getTransactionType() : "DEPOSIT");
            transaction.setPaymentMethod(dto.getPaymentMethod());
            transaction.setStatus("PENDING");
            transaction.setTransactionId(generatedTxnId);
            transaction.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
        } else {
            return Collections.singletonMap("message", "Phương thức thanh toán không hợp lệ!");
        }

        return Collections.singletonMap("redirectUrl", redirectUrl);
    }

    @Override
    public Page<TransactionDTO> getAllTransactions(Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findAll(pageable);

        return transactions.map(tx -> {
            double itemPrice = tx.getAuction().getCurrentPrice().doubleValue();
            double depositAmount = Math.max(10000, itemPrice * 0.1);
            String transactionType = tx.getAmount() >= itemPrice - depositAmount ? "FINAL" : "DEPOSIT";

            return new TransactionDTO(
                    tx.getId(),
                    tx.getCustomer().getCustomerId(),
                    tx.getAuction().getAuctionId(),
                    tx.getAmount(),
                    getTransactionTypeInVietnamese(transactionType), // ✅ Chuyển sang Tiếng Việt
                    getPaymentMethodInVietnamese(tx.getPaymentMethod()), // ✅ Chuyển sang Tiếng Việt
                    getStatusInVietnamese(tx.getStatus()), // ✅ Chuyển sang Tiếng Việt
                    tx.getCreatedAt(),
                    "",
                    tx.getCustomer().getName(),
                    tx.getAuction().getProduct().getName()

            );
        });
    }
    private String getTransactionTypeInVietnamese(String transactionType) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("FINAL", "Thanh toán đầy đủ");
        typeMap.put("DEPOSIT", "Đặt cọc");
        return typeMap.getOrDefault(transactionType, transactionType);
    }

    private String getPaymentMethodInVietnamese(String paymentMethod) {
        Map<String, String> methodMap = new HashMap<>();
        methodMap.put("PAYPAL", "Thanh toán qua PayPal");
        methodMap.put("VNPAY", "Thanh toán qua VNPAY");
        return methodMap.getOrDefault(paymentMethod, paymentMethod);
    }

    private String getStatusInVietnamese(String status) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("PENDING", "Chờ xử lý");
        statusMap.put("SUCCESS", "Thành công");
        statusMap.put("FAILED", "Thất bại");
        return statusMap.getOrDefault(status, status);
    }

    @Override
    public List<Map<String, Object>> getTotalTransactionsByDay(int days) {
        List<Object[]> results = transactionRepository.sumTransactionsByDay(days);
        System.out.println("🔍 Kết quả SQL trả về: " + results); // Debug danh sách kết quả

        List<Map<String, Object>> transactionStats = new ArrayList<>();

        for (Object[] row : results) {
            System.out.println("🟢 Row data: " + Arrays.toString(row)); // Debug từng hàng

            Map<String, Object> data = new HashMap<>();
            data.put("date", row[0]); // Ngày giao dịch

            // Kiểm tra kiểu dữ liệu của totalAmount
            if (row[1] instanceof BigDecimal) {
                data.put("totalAmount", ((BigDecimal) row[1]).doubleValue());
            } else if (row[1] instanceof Number) {
                data.put("totalAmount", ((Number) row[1]).doubleValue());
            } else {
                data.put("totalAmount", 0.0); // Nếu null thì trả về 0
            }

            transactionStats.add(data);
        }

        System.out.println("✅ Dữ liệu sau xử lý: " + transactionStats); // Debug
        return transactionStats;
    }
}
