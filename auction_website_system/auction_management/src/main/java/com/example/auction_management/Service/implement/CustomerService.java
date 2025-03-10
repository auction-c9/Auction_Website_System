package com.example.auction_management.Service.implement;

import com.example.auction_management.Service.IAccountService;
import com.example.auction_management.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService implements IAccountService {
    @Autowired
    private AccountService accountService;
    @Override
    public Optional<Account> findAccountByUsername(String username) {
        return accountService.findAccountByUsername(username);
    }
}
