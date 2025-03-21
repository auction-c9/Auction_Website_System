package com.example.auction_management.service.impl;

import com.example.auction_management.model.Account;
import com.example.auction_management.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.DisabledException;


import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Kiểm tra violation_count
        if (account.getViolationCount() != null && account.getViolationCount() >= 3) {
            account.setLocked(true);
            accountRepository.save(account);

            throw new DisabledException("Tài khoản của bạn đã bị khóa do vi phạm quá 3 lần. Vui lòng liên hệ Admin!");
        }

        if (account.isLocked()) {
            throw new DisabledException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin!");
        }

        // Lấy role từ account
        String roleName = account.getRole().getName();

        // Tạo GrantedAuthority từ role
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(roleName)
        );

        return new User(
                account.getUsername(),
                account.getPassword(),
                authorities
        );
    }

}
