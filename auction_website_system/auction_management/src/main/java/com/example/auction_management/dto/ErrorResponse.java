package com.example.auction_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor // ✅ Bổ sung để dễ dàng khởi tạo rỗng khi cần
@Builder
public class ErrorResponse {

    private int statusCode;             // Mã lỗi (HTTP Status Code)
    private String message;             // Thông điệp lỗi tổng quát
    private LocalDateTime timestamp;    // Thời gian lỗi xảy ra
    private Map<String, String> errors; // Chi tiết lỗi theo field (nếu có - dùng cho validation)

    // ✅ (Optional) Có thể thêm phương thức tiện ích tạo nhanh ErrorResponse không có errors nếu muốn
    public static ErrorResponse of(int statusCode, String message) {
        return ErrorResponse.builder()
                .statusCode(statusCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ✅ (Optional) Có thể thêm phương thức tiện ích tạo nhanh ErrorResponse có errors (validation)
    public static ErrorResponse of(int statusCode, String message, Map<String, String> errors) {
        return ErrorResponse.builder()
                .statusCode(statusCode)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
