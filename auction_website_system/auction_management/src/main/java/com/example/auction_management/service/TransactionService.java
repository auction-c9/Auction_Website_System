package com.example.auction_management.service;

import com.example.auction_management.dto.TransactionDTO;
import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Transaction;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        return List.of();
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
            transaction.setTransactionType(dto.getTransactionType());
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
}
