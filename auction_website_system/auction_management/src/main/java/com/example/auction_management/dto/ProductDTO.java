package com.example.auction_management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductDTO {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    // Giả sử id của danh mục là kiểu Integer
    private Integer categoryId;

    private String description;

    @DecimalMin(value = "0.00", message = "Giá khởi điểm phải lớn hơn 0")
    private BigDecimal basePrice;

    private MultipartFile imageFile;
}
