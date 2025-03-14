package com.example.auction_management.service;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Transaction;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class PaypalService {

    @Value("${paypal.clientId}")
    private String clientId;

    @Value("${paypal.clientSecret}")
    private String clientSecret;

    @Value("${paypal.mode}")
    private String mode;

    @Value("${paypal.returnUrl}")
    private String returnUrl;

    @Value("${paypal.cancelUrl}")
    private String cancelUrl;

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final AuctionRepository auctionRepository;
    private final RestTemplate restTemplate;

    public PaypalService(TransactionRepository transactionRepository, CustomerRepository customerRepository, AuctionRepository auctionRepository) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.auctionRepository = auctionRepository;
        this.restTemplate = new RestTemplate();
    }

    // Sinh mã giao dịch cho PayPal
    public String generateTransactionId() {
        return UUID.randomUUID().toString();
    }

    private String getAccessToken() {
        String authUrl = "https://api.sandbox.paypal.com/v1/oauth2/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = clientId + ":" + clientSecret;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + new String(encodedAuth));
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        ResponseEntity<Map> response = restTemplate.postForEntity(authUrl, new HttpEntity<>(body, headers), Map.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().get("access_token").toString();
        }
        throw new RuntimeException("Failed to get PayPal access token");
    }

    // Tạo thanh toán PayPal và trả về redirectUrl
    public String createPayment(Integer customerId, Integer auctionId, Double amount, String transactionId) {
        // Lấy Customer và Auction (nếu cần cho mô tả, v.v.)
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // Tạo giao dịch đã được set transactionId ở controller rồi
        // (Ở đây chúng ta chỉ cần gọi API PayPal để tạo payment)

        String accessToken = getAccessToken();
        if (accessToken == null) throw new RuntimeException("Cannot retrieve PayPal access token");

        Map<String, Object> paymentPayload = new HashMap<>();
        paymentPayload.put("intent", "sale");
        Map<String, String> redirectUrls = new HashMap<>();
        redirectUrls.put("return_url", returnUrl + "?transactionId=" + transactionId);
        redirectUrls.put("cancel_url", cancelUrl + "?transactionId=" + transactionId);
        paymentPayload.put("redirect_urls", redirectUrls);
        Map<String, Object> payer = new HashMap<>();
        payer.put("payment_method", "paypal");
        paymentPayload.put("payer", payer);
        Map<String, Object> amountMap = new HashMap<>();
        double conversionRate = 23000.0;
        double usdAmount = amount / conversionRate;
        String total = String.format("%.2f", usdAmount);
        amountMap.put("total", total);
        amountMap.put("currency", "USD");
        Map<String, Object> transactionDetail = new HashMap<>();
        transactionDetail.put("amount", amountMap);
        transactionDetail.put("description", "Thanh toán đặt cọc cho phiên đấu giá");
        paymentPayload.put("transactions", Collections.singletonList(transactionDetail));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(paymentPayload, headers);
        String paymentUrl = "https://api.sandbox.paypal.com/v1/payments/payment";
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(paymentUrl, httpEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                List<Map> links = (List<Map>) body.get("links");
                for (Map link : links) {
                    if ("approval_url".equals(link.get("rel"))) {
                        return link.get("href").toString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Failed to create PayPal payment");
    }

    public String executePayment(String paymentId, String payerId) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            throw new RuntimeException("Unable to retrieve PayPal access token");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("payer_id", payerId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        String executeUrl = "https://api.sandbox.paypal.com/v1/payments/payment/" + paymentId + "/execute";
        ResponseEntity<Map> response = restTemplate.postForEntity(executeUrl, entity, Map.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return "SUCCESS";
        }
        return "FAILED";
    }

    public String handlePayPalReturn(String transactionId, String paymentId, String payerId) {
        // Gọi API execute payment
        String executionStatus = executePayment(paymentId, payerId);
        // Cập nhật trạng thái giao dịch
        Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            transaction.setStatus("SUCCESS".equalsIgnoreCase(executionStatus) ? "SUCCESS" : "FAILED");
            transactionRepository.save(transaction);
        }
        return "Payment processed via PayPal";
    }
}
