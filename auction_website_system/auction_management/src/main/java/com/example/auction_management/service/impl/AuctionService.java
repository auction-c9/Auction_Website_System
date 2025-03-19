package com.example.auction_management.service.impl;

import com.example.auction_management.dto.RegisteredAuctionDTO;
import com.example.auction_management.exception.AuctionNotFoundException;
import com.example.auction_management.exception.ResourceNotFoundException;
import com.example.auction_management.exception.UnauthorizedActionException;
import com.example.auction_management.model.*;
import com.example.auction_management.repository.*;
import com.example.auction_management.service.IAuctionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionService implements IAuctionService {

    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final BidRepository bidRepository;
    private final CustomerRepository customerRepository;

    // ------------------ CORE SERVICES ----------------------

    @Override
    public List<Auction> findAll() {
        return auctionRepository.findAll();
    }

    @Override
    public Optional<Auction> findById(Integer integer) {
        return auctionRepository.findById(integer);
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
    @Transactional
    public void updateAuctionStatuses() {
        auctionRepository.updateAuctionStatuses(LocalDateTime.now());
    }
    public List<Auction> findAllWithUpdatedStatus() {
        List<Auction> auctions = auctionRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : auctions) {
            if (auction.getAuctionStartTime() == null || auction.getAuctionEndTime() == null) {
                System.out.println("Lỗi: Phiên đấu giá ID " + auction.getAuctionId() + " có thời gian null.");
                continue; // Bỏ qua nếu dữ liệu không hợp lệ
            }

            if (now.isBefore(auction.getAuctionStartTime())) {
                auction.setStatus(Auction.AuctionStatus.pending);
            } else if (now.isAfter(auction.getAuctionStartTime()) && now.isBefore(auction.getAuctionEndTime())) {
                auction.setStatus(Auction.AuctionStatus.active);
            } else {
                auction.setStatus(Auction.AuctionStatus.ended);
            }
        }

        return auctionRepository.saveAll(auctions);
    }

    public List<RegisteredAuctionDTO> getRegisteredAuctionsByCustomerId(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Account account = customer.getAccount();

        List<Product> products = productRepository.findByAccountAndIsDeletedFalse(account);
        List<Auction> auctions = auctionRepository.findByProductInAndIsDeletedFalse(products);

        return auctions.stream().map(auction -> {
            RegisteredAuctionDTO dto = new RegisteredAuctionDTO();
            dto.setAuctionId(auction.getAuctionId());
            dto.setProductName(auction.getProduct().getName());
            dto.setProductDescription(auction.getProduct().getDescription());
            dto.setBasePrice(auction.getProduct().getBasePrice());
            dto.setAuctionStartTime(auction.getAuctionStartTime());
            dto.setAuctionEndTime(auction.getAuctionEndTime());
            dto.setStatus(auction.getStatus());
            dto.setCreatedAt(auction.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void cancelAuction(Integer auctionId, Integer customerId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (!auction.getProduct().getAccount().getAccountId().equals(customer.getAccount().getAccountId())) {
            throw new AccessDeniedException("You do not have permission to cancel this auction");
        }

        auction.setIsDeleted(true);
        auctionRepository.save(auction);
    }
}