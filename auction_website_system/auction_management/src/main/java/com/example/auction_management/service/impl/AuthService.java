package com.example.auction_management.service.impl;

import com.example.auction_management.dto.AccountDto;
import com.example.auction_management.dto.JwtResponse;
import com.example.auction_management.dto.LoginRequest;
import com.example.auction_management.model.Account;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Role;
import com.example.auction_management.repository.AccountRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.RoleRepository;
import com.example.auction_management.util.JwtTokenProvider;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.query.Param;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;

    public AuthService(AuthenticationManager authenticationManager, CustomUserDetailsService customUserDetailsService,
                       JwtTokenProvider jwtTokenProvider, AccountRepository accountRepository, PasswordEncoder passwordEncoder, CustomerRepository customerRepository, RoleRepository roleRepository) {
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
    }

    public JwtResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        Optional<Integer> optionalCustomerId = customerRepository.findCustomerIdByUsername(loginRequest.getUsername());
        Integer customerId = optionalCustomerId.orElse(null);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(loginRequest.getUsername());
        String token = jwtTokenProvider.generateToken(userDetails.getUsername(),customerId);
        return new JwtResponse(token,customerId);
    }

    public void register(AccountDto accountDto, String storedCaptchaAnswer) {
        System.out.println("[DEBUG] Password: " + accountDto.getPassword());
        System.out.println("[DEBUG] Confirm Password: " + accountDto.getConfirmPassword());
        if (accountDto.getPassword() == null || accountDto.getConfirmPassword() == null) {
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không được để trống");
        }

        if (!accountDto.getPassword().equals(accountDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu không khớp");
        }
        // Xác thực mã
        if (!accountDto.getCaptcha().equals(storedCaptchaAnswer)) {
            throw new IllegalArgumentException("Mã xác thực không chính xác");
        }
        // Kiểm tra username/email trùng
        if (accountRepository.existsByUsername(accountDto.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        // Tạo Account
        Account account = new Account();
        account.setUsername(accountDto.getUsername());
        account.setPassword(passwordEncoder.encode(accountDto.getPassword()));
        account.setStatus(Account.AccountStatus.active);
        // Lấy Role từ database (sửa thành "ROLE_USER")
        Role defaultRole = roleRepository.findByName("ROLE_USER") // Sửa ở đây
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
        account.setRole(defaultRole);

        // Tạo Customer
        Customer customer = new Customer();
        customer.setName(accountDto.getName());
        customer.setEmail(accountDto.getEmail());
        customer.setPhone(accountDto.getPhone());
        customer.setIdentityCard(accountDto.getIdentityCard());
        customer.setAddress(accountDto.getAddress());
        customer.setAccount(account);

        // Lưu vào database
        accountRepository.save(account);
        customerRepository.save(customer);
    }
}
