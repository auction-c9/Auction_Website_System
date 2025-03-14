package com.example.auction_management.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.auction_management.config.CloudinaryConfig;
import com.example.auction_management.dto.AccountDto;
import com.example.auction_management.dto.JwtResponse;
import com.example.auction_management.dto.LoginRequest;
import com.example.auction_management.model.Account;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Image;
import com.example.auction_management.model.Role;
import com.example.auction_management.repository.AccountRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.ImageRepository;
import com.example.auction_management.repository.RoleRepository;
import com.example.auction_management.util.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.query.Param;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {
    private Cloudinary cloudinary = CloudinaryConfig.getCloudinary();
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final ImageRepository imageRepository;


    public AuthService(AuthenticationManager authenticationManager, CustomUserDetailsService customUserDetailsService,
                       JwtTokenProvider jwtTokenProvider, AccountRepository accountRepository, PasswordEncoder passwordEncoder,
                       CustomerRepository customerRepository, RoleRepository roleRepository,ImageRepository imageRepository) {
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
        this.imageRepository = imageRepository;
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

        // Upload ảnh đại diện
        Image avatar = null;
        if (accountDto.getAvatarFile() != null && !accountDto.getAvatarFile().isEmpty()) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                        accountDto.getAvatarFile().getBytes(),
                        ObjectUtils.emptyMap()
                );

                avatar = new Image();
                avatar.setImageUrl(uploadResult.get("url").toString());
                imageRepository.save(avatar);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi khi upload ảnh đại diện");
            }
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
        customer.setAvatar(avatar);
        customer.setAccount(account);

        // Lưu vào database
        accountRepository.save(account);
        customerRepository.save(customer);
    }

    public JwtResponse handleGoogleLogin(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        // Kiểm tra tài khoản đã tồn tại
        Optional<Account> existingAccount = accountRepository.findByUsername(email);
        if (existingAccount.isPresent()) {
            return handleExistingAccount(existingAccount.get());
        }

        // Tạo tài khoản mới
        Account newAccount = createSocialAccount(email);
        Customer newCustomer = createSocialCustomer(newAccount, name, email, pictureUrl);

        // Tạo JWT
        String token = jwtTokenProvider.generateToken(email, newCustomer.getCustomerId());
        return new JwtResponse(token, newCustomer.getCustomerId());
    }

    private JwtResponse handleExistingAccount(Account account) {
        Customer customer = customerRepository.findByAccount(account)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        if (account.getStatus() != Account.AccountStatus.active) {
            throw new RuntimeException("Tài khoản chưa được kích hoạt");
        }

        String token = jwtTokenProvider.generateToken(account.getUsername(), customer.getCustomerId());
        return new JwtResponse(token, customer.getCustomerId());
    }

    private Account createSocialAccount(String email) {
        Account account = new Account();
        account.setUsername(email);
        String randomPassword = UUID.randomUUID().toString();
        account.setPassword(passwordEncoder.encode(randomPassword));
        account.setStatus(Account.AccountStatus.active);
        account.setRole(roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò người dùng")));
        return accountRepository.save(account);
    }

    private Customer createSocialCustomer(Account account, String name, String email, String pictureUrl) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        customer.setAccount(account);

        if (pictureUrl != null) {
            Image avatar = new Image();
            avatar.setImageUrl(pictureUrl);
            imageRepository.save(avatar);
            customer.setAvatar(avatar);
        }

        return customerRepository.save(customer);
    }
}
