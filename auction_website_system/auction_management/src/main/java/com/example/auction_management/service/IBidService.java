package com.example.auction_management.service;

import com.example.auction_management.dto.BidDTO;
import com.example.auction_management.dto.BidResponseDTO;
import com.example.auction_management.model.Bid;


public interface IBidService extends IService<Bid, Integer> {
    BidResponseDTO placeBid(BidDTO bidDTO);
}
