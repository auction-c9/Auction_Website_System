package com.example.auction_management.mapper;

import com.example.auction_management.dto.AuctionDTO;
import com.example.auction_management.model.Auction;
import org.springframework.stereotype.Component;

@Component
public class AuctionMapper {
    public AuctionDTO toDTO(Auction auction) {
        AuctionDTO dto = new AuctionDTO();
        dto.setAuctionId(auction.getAuctionId());
        dto.setProductName(auction.getProduct().getName());
        dto.setHighestBid(auction.getHighestBid());
        dto.setAuctionEndTime(auction.getAuctionEndTime());
        return dto;
    }
}
