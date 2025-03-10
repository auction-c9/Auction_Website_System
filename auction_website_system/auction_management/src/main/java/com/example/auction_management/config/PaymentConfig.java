package com.example.auction_management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {

    private PayPalConfig paypal;
    private VNPayConfig vnpay;

    public PayPalConfig getPaypal() {
        return paypal;
    }
    public void setPaypal(PayPalConfig paypal) {
        this.paypal = paypal;
    }
    public VNPayConfig getVnpay() {
        return vnpay;
    }
    public void setVnpay(VNPayConfig vnpay) {
        this.vnpay = vnpay;
    }

    public static class PayPalConfig {
        private String clientId;
        private String clientSecret;
        private String mode;
        public String getClientId() {
            return clientId;
        }
        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
        public String getClientSecret() {
            return clientSecret;
        }
        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
        public String getMode() {
            return mode;
        }
        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class VNPayConfig {
        private String tmnCode;
        private String hashSecret;
        private String payUrl;
        private String returnUrl;
        public String getTmnCode() {
            return tmnCode;
        }
        public void setTmnCode(String tmnCode) {
            this.tmnCode = tmnCode;
        }
        public String getHashSecret() {
            return hashSecret;
        }
        public void setHashSecret(String hashSecret) {
            this.hashSecret = hashSecret;
        }
        public String getPayUrl() {
            return payUrl;
        }
        public void setPayUrl(String payUrl) {
            this.payUrl = payUrl;
        }
        public String getReturnUrl() {
            return returnUrl;
        }
        public void setReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
        }
    }
}

