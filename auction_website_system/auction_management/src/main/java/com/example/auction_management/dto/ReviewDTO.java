package com.example.auction_management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewDTO {
    private Integer id;
    private String buyerName;
    private String productName;
    @Min(1)
    @Max(5)
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
