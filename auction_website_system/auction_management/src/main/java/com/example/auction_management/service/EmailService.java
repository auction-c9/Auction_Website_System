package com.example.auction_management.service;

import com.example.auction_management.exception.EmailException;
import com.example.auction_management.model.Customer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}") // Lấy từ application.properties
    private String fromEmail;

    public void sendVerificationEmail(String toEmail, String verificationLink) {
        try {
            // 1. Tạo MIME message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 2. Chuẩn bị template context
            Context context = new Context();
            context.setVariable("verificationLink", verificationLink);

            // 3. Render HTML từ template
            String htmlContent = templateEngine.process("email-verification", context);

            // 4. Thiết lập email
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔗 Xác nhận liên kết tài khoản");
            helper.setText(htmlContent, true); // true = HTML content

            // 5. Gửi email
            mailSender.send(mimeMessage);
            logger.info("📩 Đã gửi email xác nhận đến: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("❌ Lỗi tạo email: {}", e.getMessage());
            throw new EmailException("Lỗi hệ thống khi tạo email");
        } catch (MailAuthenticationException e) {
            logger.error("🔐 Lỗi xác thực email: {}", e.getMessage());
            throw new EmailException("Sai thông tin xác thực email");
        } catch (MailSendException e) {
            logger.error("🚫 Lỗi gửi email: {}", e.getMessage());
            throw new EmailException("Không thể gửi email");
        }
    }

    public void sendPasswordResetEmail(String toEmail, Customer customer, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            Context context = new Context();
            context.setVariable("customer", customer);
            context.setVariable("code", code);

            String htmlContent = templateEngine.process("password-reset-email", context);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔑 Đặt lại mật khẩu");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            logger.info("📩 Đã gửi email đặt lại mật khẩu đến: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("❌ Lỗi gửi email: {}", e.getMessage());
            throw new EmailException("Không thể gửi email: " + e.getMessage());
        }
    }

    public void sendEmail(String to, String subject, String body, boolean isHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("daugiavn123@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, isHtml);

            mailSender.send(message);

        } catch (MailAuthenticationException e) {
            throw new MailAuthenticationException("Lỗi xác thực email. Vui lòng kiểm tra tài khoản gửi email!", e);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể tạo email, vui lòng thử lại!", e);
        } catch (MailException e) {
            throw new RuntimeException("Lỗi trong quá trình gửi email, vui lòng thử lại!", e);
        }
    }

}