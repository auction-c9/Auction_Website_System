package com.example.auction_management.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Period;

@Getter
@Setter
@NoArgsConstructor
public class CustomerDTO {
    private String username; // Tên đăng nhập (từ Account)

    @NotBlank(message = "Họ tên không được để trống")
    private String name;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @Past(message = "Ngày sinh không hợp lệ")
    private LocalDate dob;

    @Pattern(regexp = "\\d{10,15}", message = "Số điện thoại không hợp lệ")
    private String phone;

    private String identityCard;

    private String address;

    private String currentPassword;

    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String newPassword;

    private String confirmPassword;

    private MultipartFile avatarFile; // File ảnh upload
    private String avatarUrl; // URL từ Cloudinary

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

