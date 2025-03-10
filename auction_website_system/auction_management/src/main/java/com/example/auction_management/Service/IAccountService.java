package com.example.auction_management.Service;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Role;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

public interface IAccountService {
    Optional<Account> findAccountByUsername(String username);
}
