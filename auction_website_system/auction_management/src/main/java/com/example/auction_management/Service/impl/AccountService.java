package com.example.auction_management.service.impl;

import com.example.auction_management.Service.IAccountService;
import com.example.auction_management.model.Account;
import com.example.auction_management.repository.AccountRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountService implements IAccountService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;


    @Override
    public Optional<Account> findAccountByUsername(String username) {
        return accountRepository.findByUsername(username);
    }
}
