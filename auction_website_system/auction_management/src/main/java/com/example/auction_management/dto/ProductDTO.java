package com.example.auction_management.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductDTO {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotNull(message = "Danh mục không được để trống")
    private Integer categoryId;

    private String description;

    @DecimalMin(value = "0.01", inclusive = false, message = "Giá khởi điểm phải lớn hơn 0")
    @NotNull(message = "Giá khởi điểm không được để trống")
    private BigDecimal basePrice;

    // Bỏ trường imageFile, chỉ dùng 1 trường cho nhiều ảnh
    @NotEmpty(message = "Danh sách ảnh sản phẩm không được để trống")
    private List<@NotNull(message = "Ảnh sản phẩm không được để trống") MultipartFile> imageFiles;

    @NotNull(message = "Thời gian bắt đầu đấu giá không được để trống")
    @Future(message = "Thời gian bắt đầu phải trong tương lai")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private LocalDateTime auctionStartTime;

    @NotNull(message = "Thời gian kết thúc đấu giá không được để trống")
    @Future(message = "Thời gian kết thúc phải trong tương lai")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private LocalDateTime auctionEndTime;

    @DecimalMin(value = "0.01", message = "Bước giá phải lớn hơn 0")
    @NotNull(message = "Bước giá không được để trống")
    private BigDecimal bidStep;

    @Pattern(regexp = "pending|active|ended", message = "Trạng thái không hợp lệ (pending, active, ended)")
    private String status = "pending";

    private List<Integer> customerIds;
}
