package com.example.auction_management.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

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

    @NotNull(message = "Ảnh đại diện sản phẩm không được để trống")
    private MultipartFile imageFile;

    @NotEmpty(message = "Danh sách ảnh chi tiết không được để trống")
    private List<@NotNull(message = "Ảnh chi tiết không được để trống") MultipartFile> imageFiles;

    @NotNull(message = "Thời gian bắt đầu đấu giá không được để trống")
    @Future(message = "Thời gian bắt đầu phải trong tương lai")
    private LocalDateTime auctionStartTime;

    @NotNull(message = "Thời gian kết thúc đấu giá không được để trống")
    @Future(message = "Thời gian kết thúc phải trong tương lai")
    private LocalDateTime auctionEndTime;

    @DecimalMin(value = "0.01", message = "Bước giá phải lớn hơn 0")
    @NotNull(message = "Bước giá không được để trống")
    private BigDecimal bidStep;

    @Pattern(regexp = "pending|active|ended", message = "Trạng thái không hợp lệ (pending, active, ended)")
    private String status = "pending";

    private List<Integer> customerIds;
}
