package com.example.auction_management.service;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Auction.AuctionStatus;
import com.example.auction_management.model.Product;

import java.util.List;
import java.util.Optional;

public interface IAuctionService extends IService<Auction, Integer> {
    List<Auction> findByStatus(AuctionStatus status);
    Optional<Auction> findByProduct(Product product);

    List<Auction> findOngoingAuctions();
}
