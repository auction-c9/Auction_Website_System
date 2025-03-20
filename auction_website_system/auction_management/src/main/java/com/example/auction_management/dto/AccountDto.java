package com.example.auction_management.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Period;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
    @NotBlank(message = "Tên không được để trống")
    private String name;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @Pattern(regexp = "\\d{10,15}", message = "Số điện thoại phải có từ 10 đến 15 chữ số")
    private String phone;

    @NotBlank(message = "Số CMND/CCCD không được để trống")
    private String identityCard;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    @NotBlank(message = "Mã xác thực không được để trống")
    private String captcha; // Câu trả lời từ người dùng

    private String captchaQuestion;

    @NotNull(message = "Ảnh đại diện không được để trống")
    private MultipartFile avatarFile;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dob;

    @NotBlank(message = "Số tài khoản không được để trống")
    @Pattern(regexp = "\\d{10,20}", message = "Số tài khoản phải có từ 10 đến 20 chữ số")
    private String bankAccount;

    @NotBlank(message = "Tên ngân hàng không được để trống")
    private String bankName;

    @AssertTrue(message = "Bạn phải đủ 18 tuổi")
    public boolean isAdult() {
        if (dob == null) return false;
        return Period.between(dob, LocalDate.now()).getYears() >= 18;
    }

}
