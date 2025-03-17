package com.example.auction_management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class BidRequestDTO {
    @DecimalMin(value = "0.01", message = "Giá đấu phải lớn hơn 0")
    @NotNull(message = "Giá đấu không được để trống")
    private BigDecimal bidAmount;
}
