package com.example.auction_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private int statusCode;       // Mã lỗi
    private String message;       // Thông điệp lỗi
    private LocalDateTime timestamp; // Thời gian xảy ra lỗi
    private Map<String, String> errors; // Chi tiết lỗi cho từng field (nếu có, thường dùng cho validation)
}
