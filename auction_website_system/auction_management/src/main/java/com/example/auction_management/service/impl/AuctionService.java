package com.example.auction_management.service.impl;

import com.example.auction_management.exception.AuctionNotFoundException;
import com.example.auction_management.exception.UnauthorizedActionException;
import com.example.auction_management.model.*;
import com.example.auction_management.repository.*;
import com.example.auction_management.service.IAuctionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuctionService implements IAuctionService {

    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final BidRepository bidRepository;

    // ------------------ CORE SERVICES ----------------------

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
        Auction auction = getAuctionByIdAndCheckOwner(id);
        auctionRepository.deleteById(auction.getAuctionId());
    }

    @Override
    public List<Auction> findByStatus(Auction.AuctionStatus status) {
        return auctionRepository.findByStatus(status);
    }

    // Sửa kiểu trả về thành Optional nếu mỗi sản phẩm chỉ có 1 phiên đấu giá
    @Override
    public Optional<Auction> findByProduct(Product product) {
        return auctionRepository.findByProduct(product);
    }

    @Override
    public List<Auction> findOngoingAuctions() {
        return auctionRepository.findOngoingAuctions(LocalDateTime.now());
    }

    // ------------------ EXTENDED FUNCTION ----------------------

    /**
     * Kết thúc phiên đấu giá theo quyền sở hữu
     */
    @Transactional
    public void endAuction(Integer auctionId) {
        Auction auction = getAuctionByIdAndCheckOwner(auctionId);

        if (auction.getStatus().equals(Auction.AuctionStatus.ended)) {
            throw new IllegalStateException("Phiên đấu giá đã kết thúc.");
        }

        auction.setStatus(Auction.AuctionStatus.ended);
        auctionRepository.save(auction);
    }

    /**
     * Xác nhận người chiến thắng phiên đấu giá
     */
    @Transactional
    public void confirmWinner(Integer auctionId, Integer accountId) {
        Auction auction = getAuctionByIdAndCheckOwner(auctionId);

        boolean isBidValid = auction.getBids().stream()
                .anyMatch(bid -> bid.getAccount().getAccountId().equals(accountId));

        if (!isBidValid) {
            throw new UnauthorizedActionException("Người chiến thắng không hợp lệ với phiên đấu giá này.");
        }

        // Reset tất cả bid về false
        auction.getBids().forEach(bid -> bid.setIsWinner(false));

        // Gán người thắng
        auction.getBids().stream()
                .filter(bid -> bid.getAccount().getAccountId().equals(accountId))
                .forEach(bid -> bid.setIsWinner(true));

        bidRepository.saveAll(auction.getBids());
    }

    /**
     * Tìm các phiên đấu giá theo sản phẩm mà người dùng hiện tại sở hữu
     */
    public List<Auction> findMyAuctions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return auctionRepository.findByProduct_Account_Username(username);
    }

    /**
     * Lấy lịch sử đấu giá của 1 phiên đấu giá
     */
    public List<Bid> getAuctionHistory(Integer auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Không tìm thấy phiên đấu giá!"));

        return auction.getBids();
    }

    // ------------------ PRIVATE SUPPORT METHODS ----------------------

    /**
     * Lấy phiên đấu giá và kiểm tra quyền sở hữu
     */
    private Auction getAuctionByIdAndCheckOwner(Integer auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Không tìm thấy phiên đấu giá với ID: " + auctionId));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!auction.getProduct().getAccount().getUsername().equals(username)) {
            throw new UnauthorizedActionException("Bạn không có quyền trên phiên đấu giá này.");
        }
        return auction;
    }
}
