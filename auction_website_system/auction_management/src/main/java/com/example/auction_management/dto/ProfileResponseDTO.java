package com.example.auction_management.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponseDTO {
    private String avatarUrl;
    private String username;
    private String fullName;
    private List<AuctionInfoDto> auctions;
    private BigDecimal averageRating;
    private List<ReviewDTO> reviews;

    public ProfileResponseDTO(String avatarUrl, String username, String fullName,List<AuctionInfoDto> auctions){
        this.avatarUrl = avatarUrl;
        this.username = username;
        this.fullName = fullName;
        this.auctions = new ArrayList<>();


    }
}
