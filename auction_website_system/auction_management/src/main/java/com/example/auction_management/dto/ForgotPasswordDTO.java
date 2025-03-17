package com.example.auction_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordDTO {
    @NotBlank(message = "Username không được để trống", groups = {Step1.class, Step2.class})
    private String username;

    @NotBlank(message = "Mã xác nhận không được để trống", groups = {Step2.class})
    private String code;

    @NotBlank(message = "Mật khẩu mới không được để trống", groups = {Step3.class})
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự", groups = {Step3.class})
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống", groups = {Step3.class})
    private String confirmPassword;

    // Validation groups để phân biệt các bước
    public interface Step1 {} // Chỉ validate username
    public interface Step2 {} // Validate username + code
    public interface Step3 {} // Validate password
}
