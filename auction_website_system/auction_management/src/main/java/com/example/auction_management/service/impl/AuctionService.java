package com.example.auction_management.service.impl;

import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Auction.AuctionStatus;
import com.example.auction_management.model.Product;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.service.IAuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuctionService implements IAuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Override
    public List<Auction> findAll() {
        return auctionRepository.findAll();
    }

    @Override
    public Optional<Auction> findById(Integer id) {
        return auctionRepository.findById(id);
    }

    @Override
    public Auction save(Auction auction) {
        return auctionRepository.save(auction);
    }

    @Override
    public void deleteById(Integer id) {
        auctionRepository.deleteById(id);
    }

    @Override
    public List<Auction> findByStatus(AuctionStatus status) {
        return auctionRepository.findByStatus(status);
    }

    @Override
    public List<Auction> findByProduct(Product product) {
        return auctionRepository.findByProduct(product);
    }

    @Override
    public List<Auction> findOngoingAuctions() {
        return auctionRepository.findOngoingAuctions(LocalDateTime.now());
    }
}
