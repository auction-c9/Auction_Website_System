package com.example.auction_management.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Integer customerId;
    private String role;
    private String username;

    public JwtResponse(String token,Integer customerId, String role,String username) {
        this.token = token;
        this.customerId = customerId;
        this.role = role;
        this.type = "Bearer";
        this.username = username;
    }
}
