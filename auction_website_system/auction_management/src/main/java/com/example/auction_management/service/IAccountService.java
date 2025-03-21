package com.example.auction_management.service;

import com.example.auction_management.model.Account;

import java.util.Map;
import java.util.Optional;

public interface IAccountService {
    void sendWarningEmail(Integer accountId);

    Optional<Account> findAccountByUsername(String username);

    boolean lockAccount(Integer accountId);

    boolean unlockAccount(Integer accountId);

    boolean updateAccountLockStatus(Integer accountId, boolean lockStatus);
}
