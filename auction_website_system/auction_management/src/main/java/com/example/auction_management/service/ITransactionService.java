package com.example.auction_management.service;

import com.example.auction_management.dto.TransactionDTO;
import com.example.auction_management.model.Transaction;

import java.util.Map;

public interface ITransactionService extends IService<Transaction, Integer> {
    Map<String, String> createTransaction(TransactionDTO dto);
}
