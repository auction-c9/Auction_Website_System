package com.example.auction_management.service.impl;

import com.example.auction_management.model.Account;
import com.example.auction_management.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Account> account = accountRepository.findByUsername(username);
        if (account.isEmpty()) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        // Gán quyền mặc định ROLE_USER cho tất cả người dùng
        return new User(
                account.get().getUsername(),
                account.get().getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
