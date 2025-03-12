package com.example.auction_management.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
    @NotBlank
    private String name;

    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @Pattern(regexp = "\\d{10,15}")
    private String phone;

    private String identityCard;
    private String address;

    @NotBlank
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotBlank(message = "Mã xác thực không được để trống")
    private String captcha; // Câu trả lời từ người dùng

    private String captchaQuestion;
}
