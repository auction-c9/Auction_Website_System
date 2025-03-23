package com.example.auction_management.service.impl;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Customer;
import com.example.auction_management.repository.AccountRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.RoleRepository;
import com.example.auction_management.service.EmailService;
import com.example.auction_management.service.IAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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

    @Autowired
    private EmailService emailService;

    @Override
    public void sendWarningEmail(Integer accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        Customer customer = account.getCustomer();
        if (customer == null || customer.getEmail() == null) {
            throw new RuntimeException("Không tìm thấy email của tài khoản");
        }

        emailService.sendEmail(customer.getEmail(), "Cảnh báo vi phạm nội dung",
                "Bạn đã vi phạm nội dung sản phẩm. Vui lòng chỉnh sửa để tránh bị khóa tài khoản.");
    }

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
    @Override
    public List<Map<String, Object>> getNewUsersByDay(int days) {
        LocalDateTime startDateTime = LocalDate.now().minusDays(days - 1).atStartOfDay(); // Chuyển thành 00:00:00

        List<Object[]> results = accountRepository.countNewUsersPerDay(startDateTime);

        List<Map<String, Object>> response = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> data = new HashMap<>();
            data.put("date", row[0]);
            data.put("count", row[1]);
            response.add(data);
        }
        return response;
    }
}
