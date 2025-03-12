package com.example.auction_management.service;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Payment;

public interface IPaymentIntegrationService {
    String createPayPalPayment(Auction auction, Payment paymentDetails, String cancelUrl, String successUrl) throws Exception;
    com.paypal.api.payments.Payment executePayPalPayment(String paymentId, String payerId) throws Exception;
}
