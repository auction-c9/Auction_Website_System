package com.example.auction_management.service.impl;

import com.example.auction_management.model.Account;
import com.example.auction_management.repository.AccountRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.RoleRepository;
import com.example.auction_management.service.IAccountService;
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

    @Override
    public boolean lockAccount(Integer accountId) {
        return updateAccountLockStatus(accountId, true);
    }

    @Override
    public boolean unlockAccount(Integer accountId) {
        return updateAccountLockStatus(accountId, false);
    }

    @Override
    public boolean updateAccountLockStatus(Integer accountId, boolean lockStatus) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            account.setLocked(lockStatus);
            account.setStatus(lockStatus ? Account.AccountStatus.inactive : Account.AccountStatus.active);
            accountRepository.save(account);
            return true;
        }
        return false;
    }

}
