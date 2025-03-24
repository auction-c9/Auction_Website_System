package com.example.auction_management.service;

import com.example.auction_management.dto.TransactionDTO;
import com.example.auction_management.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ITransactionService extends IService<Transaction, Integer> {
    Map<String, String> createTransaction(TransactionDTO dto);

    Page<TransactionDTO> getAllTransactions(Pageable pageable);

    List<Map<String, Object>> getTotalTransactionsByDay(int days);
}
