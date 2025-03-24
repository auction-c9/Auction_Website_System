package com.example.auction_management.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.auction_management.config.CloudinaryConfig;
import com.example.auction_management.dto.AccountDto;
import com.example.auction_management.dto.CustomerDTO;
import com.example.auction_management.dto.JwtResponse;
import com.example.auction_management.dto.LoginRequest;
import com.example.auction_management.exception.*;
import com.example.auction_management.model.*;
import com.example.auction_management.repository.*;
import com.example.auction_management.service.EmailService;
import com.example.auction_management.util.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static com.example.auction_management.model.Token.TokenType.ACCOUNT_VERIFICATION;

@Slf4j
@Service
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
    private final TokenRepository tokenRepository;
    private final EmailService emailService;

    public AuthService(AuthenticationManager authenticationManager, CustomUserDetailsService customUserDetailsService,
                       JwtTokenProvider jwtTokenProvider, AccountRepository accountRepository, PasswordEncoder passwordEncoder,
                       CustomerRepository customerRepository, RoleRepository roleRepository, ImageRepository imageRepository,
                       TokenRepository tokenRepository, EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
        this.imageRepository = imageRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    public JwtResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        Optional<Integer> optionalCustomerId = customerRepository.findCustomerIdByUsername(loginRequest.getUsername());
        Integer customerId = optionalCustomerId.orElse(null);

        // Lấy thông tin role
        Account account = accountRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String role = account.getRole().getName();

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(loginRequest.getUsername());

        String token = jwtTokenProvider.generateToken(loginRequest.getUsername(), customerId, role);
        return new JwtResponse(token, customerId, role, loginRequest.getUsername());
    }

    public void register(AccountDto accountDto, String storedCaptchaAnswer) {
        try {
            System.out.println("[DEBUG] Password: " + accountDto.getPassword());
            System.out.println("[DEBUG] Confirm Password: " + accountDto.getConfirmPassword());
            if (accountDto.getPassword() == null || accountDto.getConfirmPassword() == null) {
                throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không được để trống");
            }

            if (!accountDto.getPassword().equals(accountDto.getConfirmPassword())) {
                throw new IllegalArgumentException("Mật khẩu không khớp");
            }

            if (!accountDto.getCaptcha().equals(storedCaptchaAnswer)) {
                throw new IllegalArgumentException("Mã xác thực không chính xác");
            }

            if (accountRepository.existsByUsername(accountDto.getUsername())) {
                throw new IllegalArgumentException("Username đã tồn tại");
            }

            Optional<Account> existingAccount = accountRepository.findByUsername(accountDto.getEmail());
            if (existingAccount.isPresent() && existingAccount.get().getAuthProvider() == Account.AuthProvider.GOOGLE) {
                throw new IllegalArgumentException("Email đã được đăng ký bằng Google. Vui lòng đăng nhập bằng Google.");
            }

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

            Account account = new Account();
            account.setUsername(accountDto.getUsername());
            account.setPassword(passwordEncoder.encode(accountDto.getPassword()));
            account.setStatus(Account.AccountStatus.active);

            Role defaultRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
            account.setRole(defaultRole);

            Customer customer = new Customer();
            customer.setName(accountDto.getName());
            customer.setEmail(accountDto.getEmail());
            customer.setPhone(accountDto.getPhone());
            customer.setIdentityCard(accountDto.getIdentityCard());
            customer.setAddress(accountDto.getAddress());
            customer.setDob(accountDto.getDob());
            customer.setBankAccount(accountDto.getBankAccount());
            customer.setBankName(accountDto.getBankName());
            customer.setAvatar(avatar);
            customer.setAccount(account);
            account.setAuthProvider(Account.AuthProvider.LOCAL);

            accountRepository.save(account);
            customerRepository.save(customer);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private CustomerDTO mapToDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setUsername(customer.getAccount().getUsername());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setDob(customer.getDob());
        dto.setPhone(customer.getPhone());
        dto.setIdentityCard(customer.getIdentityCard());
        dto.setAddress(customer.getAddress());
        dto.setBankAccount(customer.getBankAccount());
        dto.setBankName(customer.getBankName());
        dto.setAvatarUrl(customer.getAvatar() != null ? customer.getAvatar().getImageUrl() : null);
        return dto;
    }

    public void verifyLinkAccount(String token) {
        Token verificationToken = tokenRepository.findByCode(token)
                .orElseThrow(() -> new InvalidTokenException("Token không hợp lệ"));

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(verificationToken);
            throw new TokenExpiredException("Token đã hết hạn");
        }

        Account account = accountRepository.findByUsername(verificationToken.getIdentifier())
                .orElseThrow(() -> new AccountNotFoundException("Tài khoản không tồn tại"));

        account.setAuthProvider(Account.AuthProvider.GOOGLE);
        accountRepository.save(account);
        tokenRepository.delete(verificationToken);
    }

    public JwtResponse handleGoogleLogin(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        Optional<Account> existingAccount = accountRepository.findByUsername(email);
        if (existingAccount.isPresent()) {
            Account account = existingAccount.get();
            if (account.getAuthProvider() == Account.AuthProvider.GOOGLE) {
                return handleExistingAccount(account);
            } else {
                // Tạo token và gửi email xác nhận
                String verificationToken = UUID.randomUUID().toString();
                Token tokenEntity = new Token();
                tokenEntity.setTokenType(ACCOUNT_VERIFICATION);
                tokenEntity.setIdentifier(email);
                tokenEntity.setExpiryDate(LocalDateTime.now().plusHours(24));
                tokenRepository.save(tokenEntity);

                // Gửi email xác nhận (KHÔNG trả về JWT)
                String verificationLink = "http://your-domain.com/verify-link-account?token=" + verificationToken;
                emailService.sendVerificationEmail(email, verificationLink);

                throw new AccountConflictException("Vui lòng xác nhận email trước khi đăng nhập");
            }
        }

        // Tạo tài khoản mới
        Account newAccount = createSocialAccount(email);
        Customer newCustomer = createSocialCustomer(newAccount, name, email, pictureUrl);
        String role = newAccount.getRole().getName();
        String username = newAccount.getUsername();

        return new JwtResponse(
                jwtTokenProvider.generateToken(username, newCustomer.getCustomerId(), role),
                newCustomer.getCustomerId(), role, username);
    }

    private JwtResponse handleExistingAccount(Account account) {
        Customer customer = customerRepository.findByAccount(account)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        if (account.getStatus() != Account.AccountStatus.active) {
            throw new RuntimeException("Tài khoản chưa được kích hoạt");
        }

        String role = account.getRole().getName();
        String username = account.getUsername();
        String token = jwtTokenProvider.generateToken(username, customer.getCustomerId(),role);
        return new JwtResponse(token, customer.getCustomerId(),role,username);
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
        // Kiểm tra Customer đã tồn tại bằng email
        Optional<Customer> existingCustomer = customerRepository.findByEmail(email);
        Customer customer;

        if (existingCustomer.isPresent()) {
            // Cập nhật Customer hiện có
            customer = existingCustomer.get();
            customer.setName(name);
            customer.setAccount(account); // Liên kết với Account mới (Google)
        } else {
            // Tạo mới Customer
            customer = new Customer();
            customer.setName(name);
            customer.setEmail(email);
            customer.setAccount(account);
        }

        // Xử lý avatar
        if (pictureUrl != null) {
            Image avatar = new Image();
            avatar.setImageUrl(pictureUrl);
            imageRepository.save(avatar);
            customer.setAvatar(avatar);
        }

        return customerRepository.save(customer);
    }

    @Transactional(noRollbackFor = EmailException.class)
    public void initiatePasswordReset(String username) {
        try {
            log.info("Bắt đầu quy trình quên mật khẩu cho username: {}", username);

            Optional<Account> accountOpt = accountRepository.findByUsername(username);
            if (accountOpt.isEmpty()) {
                log.error("Tài khoản không tồn tại: {}", username);
                throw new IllegalArgumentException("Tài khoản không tồn tại");
            }

            Account account = accountOpt.get();
            Customer customer = account.getCustomer();
            if (customer == null || customer.getEmail() == null) {
                log.error("Tài khoản {} không có email đăng ký", username);
                throw new IllegalArgumentException("Tài khoản không có email đăng ký");
            }
            String email = customer.getEmail();
            log.info("Email của tài khoản {}: {}", username, email);

            // Xóa token cũ bằng EMAIL
            tokenRepository.deleteByIdentifierAndTokenType(email, Token.TokenType.PASSWORD_RESET);
            log.info("Đã xóa token cũ cho email: {}", email);

            String code = String.format("%06d", new Random().nextInt(999999));
            log.info("Mã code được tạo: {}", code);

            Token token = new Token();
            token.setCode(code);
            token.setIdentifier(email);
            token.setTokenType(Token.TokenType.PASSWORD_RESET);
            token.setExpiryDate(LocalDateTime.now().plusMinutes(5));
            log.info("Thông tin token trước khi lưu: {}", token);

            // Lưu token
            Token savedToken = tokenRepository.save(token);
            log.info("Đã lưu token thành công: {}", savedToken);

            // Gửi email
            emailService.sendPasswordResetEmail(email, customer, code);
            log.info("Đã gửi email đến: {}", email);

        } catch (Exception e) {
            log.error("Lỗi trong initiatePasswordReset: ", e);
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public void validateResetCode(String username, String code) {
        // Lấy EMAIL từ username
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));
        String email = account.getCustomer().getEmail();

        Optional<Token> tokenOpt = tokenRepository.findByCodeAndIdentifierAndTokenType(
                code,
                email,
                Token.TokenType.PASSWORD_RESET
        );
        Token token = tokenOpt.orElseThrow(() ->
                new IllegalArgumentException("Mã xác nhận không hợp lệ")
        );

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(token);
            throw new IllegalArgumentException("Mã đã hết hạn");
        }
    }

    public void resetPassword(String username, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu không khớp");
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));

        // Mã hóa mật khẩu mới
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        // Xóa token đã sử dụng
        tokenRepository.deleteByIdentifierAndTokenType(username, Token.TokenType.PASSWORD_RESET);
    }

    @Transactional(readOnly = true)
    public CustomerDTO getCustomerProfile(String username) {
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        return mapToDTO(customer);
    }

    // Cập nhật profile
    @Transactional
    public CustomerDTO updateCustomerProfile(String username, CustomerDTO customerDTO) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));

        Customer customer = customerRepository.findByAccount(account)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thông tin cá nhân"));

        // Cập nhật thông tin cơ bản
        customer.setName(customerDTO.getName());
        customer.setEmail(customerDTO.getEmail());
        customer.setDob(customerDTO.getDob());
        customer.setPhone(customerDTO.getPhone());
        customer.setBankAccount(customerDTO.getBankAccount());
        customer.setBankName(customerDTO.getBankName());
        customer.setIdentityCard(customerDTO.getIdentityCard());
        customer.setAddress(customerDTO.getAddress());

        // Xử lý upload ảnh
        if (customerDTO.getAvatarFile() != null && !customerDTO.getAvatarFile().isEmpty()) {
            uploadAndUpdateAvatar(customer, customerDTO.getAvatarFile());
        }

        customerRepository.save(customer);
        return mapToDTO(customer);
    }

    private void validatePasswordChange(Account account, CustomerDTO dto) {
        if (!passwordEncoder.matches(dto.getCurrentPassword(), account.getPassword())) {
            throw new InvalidPasswordException("Mật khẩu hiện tại không đúng");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new PasswordMismatchException("Mật khẩu mới không khớp");
        }
    }

    private void uploadAndUpdateAvatar(Customer customer, MultipartFile file) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "avatars"));

            String imageUrl = (String) uploadResult.get("url");

            // Tạo hoặc cập nhật ảnh
            Image avatar = customer.getAvatar();
            if (avatar == null) {
                avatar = new Image();
                avatar.setImageUrl(imageUrl);
                imageRepository.save(avatar);
                customer.setAvatar(avatar);
            } else {
                avatar.setImageUrl(imageUrl);
                imageRepository.save(avatar);
            }
        } catch (IOException e) {
            throw new FileUploadException("Lỗi upload ảnh đại diện");
        }
    }

    public void changePassword(String username, String currentPassword,
                               String newPassword, String confirmPassword) {

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));

        // Validate password
        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            throw new InvalidPasswordException("Mật khẩu hiện tại không đúng");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new PasswordMismatchException("Mật khẩu mới không khớp");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }
}
