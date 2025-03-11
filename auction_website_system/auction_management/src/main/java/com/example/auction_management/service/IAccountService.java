package com.example.auction_management.service;

import com.example.auction_management.model.Account;

import java.util.Optional;

public interface IAccountService {
    Optional<Account> findAccountByUsername(String username);
}
