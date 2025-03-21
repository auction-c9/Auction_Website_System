package com.example.auction_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsDTO {
    private long totalCustomers;
    private long totalProducts;
    private long totalTransactions;
}
