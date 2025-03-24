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

        String emailContent = generateWarningEmailContent(customer.getName());
        String subject = "Cảnh báo vi phạm nội dung";

        emailService.sendEmail(customer.getEmail(), subject, emailContent);
    }

    private String generateWarningEmailContent(String customerName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.8; color: #333; margin: 0; padding: 0; font-size: 16px; }" +
                "        .content { padding: 25px; background-color: #f9f9f9; border-radius: 8px; max-width: 600px; margin: 20px auto; text-align: left; font-size: 18px; }" +
                "        .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 16px; color: #666; text-align: left; }" +
                "        .footer strong { color: #333; font-size: 17px; }" +
                "        .footer a { color: #007BFF; text-decoration: none; font-size: 16px; }" +
                "        .footer a:hover { text-decoration: underline; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "<div class='content'>" +
                "    <p style='font-size: 20px;'><strong>Xin chào, " + customerName + "!</strong></p>" +
                "    <p>Bạn đã vi phạm quy định về điều khoản trên hệ thống.</p>" +
                "    <p>Nếu bạn có thắc mắc, vui lòng liên hệ với chúng tôi để được hỗ trợ.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "    <p>Trân trọng,</p>" +
                "    <p><strong>C9-Stock</strong></p>" +
                "    <p>📍 Địa chỉ: 295 Nguyễn Tất Thành, Thanh Bình, Hải Châu, Đà Nẵng</p>" +
                "    <p>📞 Số điện thoại: <a href='tel:+84356789999'>+84 356789999</a></p>" +
                "    <p>✉ Email: <a href='mailto:daugiavn123@gmail.com'>daugiavn123@gmail.com</a></p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    @Override
    public Optional<Account> findAccountByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

    @Override
    public boolean lockAccount(Integer accountId) {
        boolean locked = updateAccountLockStatus(accountId, true);
        if (locked) {
            sendLockNotificationEmail(accountId);
        }
        return locked;
    }

    private void sendLockNotificationEmail(Integer accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        Customer customer = account.getCustomer();
        if (customer == null || customer.getEmail() == null) {
            throw new RuntimeException("Không tìm thấy email của tài khoản");
        }

        String emailContent = generateLockEmailContent(customer.getName());
        String subject = "Thông báo khóa tài khoản";

        emailService.sendEmail(customer.getEmail(), subject, emailContent);
    }

    private String generateLockEmailContent(String customerName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.8; color: #333; margin: 0; padding: 0; font-size: 16px; }" +
                "        .content { padding: 25px; background-color: #f9f9f9; border-radius: 8px; max-width: 600px; margin: 20px auto; text-align: left; font-size: 18px; }" +
                "        .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 16px; color: #666; text-align: left; }" +
                "        .footer strong { color: #333; font-size: 17px; }" +
                "        .footer a { color: #007BFF; text-decoration: none; font-size: 16px; }" +
                "        .footer a:hover { text-decoration: underline; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "<div class='content'>" +
                "    <p style='font-size: 20px;'><strong>Xin chào, " + customerName + "!</strong></p>" +
                "    <p>Chúng tôi xin thông báo rằng tài khoản của bạn đã bị <strong>tạm khóa</strong> do vi phạm chính sách của hệ thống.</p>" +
                "    <p>Vui lòng liên hệ với bộ phận hỗ trợ để biết thêm chi tiết hoặc yêu cầu mở khóa tài khoản.</p>" +
                "    <p>Chúng tôi xin lỗi vì sự bất tiện này.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "    <p>Trân trọng,</p>" +
                "    <p><strong>C9-Stock</strong></p>" +
                "    <p>📍 Địa chỉ: 295 Nguyễn Tất Thành, Thanh Bình, Hải Châu, Đà Nẵng</p>" +
                "    <p>📞 Số điện thoại: <a href='tel:+84356789999'>+84 356789999</a></p>" +
                "    <p>✉ Email: <a href='mailto:daugiavn123@gmail.com'>daugiavn123@gmail.com</a></p>" +
                "</div>" +
                "</body>" +
                "</html>";
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
        LocalDateTime startDateTime = LocalDate.now().minusDays(days - 1).atStartOfDay();

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