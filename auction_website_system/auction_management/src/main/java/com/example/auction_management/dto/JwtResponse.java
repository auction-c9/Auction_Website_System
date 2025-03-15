package com.example.auction_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Integer customerId;

    public JwtResponse(String token,Integer customerId) {
        this.token = token;
        this.type = "Bearer";
        this.customerId = customerId;
    }
}
