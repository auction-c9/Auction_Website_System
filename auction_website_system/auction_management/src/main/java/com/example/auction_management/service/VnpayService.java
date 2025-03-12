package com.example.auction_management.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VnpayService {
    private static final String VNP_TMNCODE = "A3GXRGWV";
    public static final String VNP_HASHSECRET = "CKF45LLG32RI8JVIWPXWVBZ1U7COEU8M";
    private static final String VNP_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    public static String generatePaymentUrl(String returnUrl, double amount, String orderInfo, String orderId) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_TmnCode", VNP_TMNCODE);
        params.put("vnp_Amount", String.valueOf((int) (amount * 100)));
        params.put("vnp_Command", "pay");
        params.put("vnp_CreateDate", getCurrentDate());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_Locale", "vn");
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "billpayment");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_TxnRef", orderId);

        // Sắp xếp tham số theo thứ tự A-Z
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String value = params.get(fieldName);
            if (value != null && !value.isEmpty()) {
                try {
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()))
                            .append("=")
                            .append(URLEncoder.encode(value, StandardCharsets.UTF_8.toString()))
                            .append("&");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        String queryUrl = query.substring(0, query.length() - 1);
        String secureHash = hmacSHA512(queryUrl, VNP_HASHSECRET);
        return VNP_URL + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;
    }

    private static String getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        return String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", calendar);
    }

    public static String hmacSHA512(String data, String secretKey) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Chuyển bytes thành hex thủ công (thay thế DatatypeConverter)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02X", b)); // "%02X" để đảm bảo luôn có 2 chữ số
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo HMAC-SHA512", e);
        }
    }

    public String createQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }
}

