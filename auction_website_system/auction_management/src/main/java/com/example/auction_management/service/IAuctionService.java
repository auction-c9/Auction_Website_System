package com.example.auction_management.service;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Auction.AuctionStatus;
import com.example.auction_management.model.Product;

import java.util.List;

public interface IAuctionService extends IService<Auction, Integer> {
    List<Auction> findByStatus(AuctionStatus status);
    List<Auction> findByProduct(Product product);
    List<Auction> findOngoingAuctions();
}
