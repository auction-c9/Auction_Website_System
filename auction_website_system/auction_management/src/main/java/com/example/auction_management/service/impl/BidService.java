package com.example.auction_management.service.impl;

import com.example.auction_management.dto.BidDTO;
import com.example.auction_management.dto.BidResponseDTO;
import com.example.auction_management.exception.*;
import com.example.auction_management.model.*;
import com.example.auction_management.repository.*;
import com.example.auction_management.service.IBidService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BidService implements IBidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    // ---------------------- CRUD BASIC ----------------------

    @Override
    public List<Bid> findAll() {
        return bidRepository.findAll();
    }

    @Override
    public Optional<Bid> findById(Integer id) {
        return bidRepository.findById(id);
    }

    @Override
    public Bid save(Bid bid) {
        return bidRepository.save(bid);
    }

    @Override
    public void deleteById(Integer id) {
        Bid bid = bidRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giá thầu với ID: " + id));
        bidRepository.delete(bid);
    }

    // ---------------------- PLACE BID LOGIC ----------------------
    @Override
    @Transactional
    public BidResponseDTO placeBid(BidDTO bidDTO) {
        Auction auction = auctionRepository.findById(bidDTO.getAuctionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá!"));

        validateAuctionStatus(auction);

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Customer customer = customerRepository.findByAccountUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng!"));

        if (auction.getProduct().getAccount().getAccountId().equals(customer.getAccount().getAccountId())) {
            throw new InvalidActionException("Bạn không thể đặt giá cho bài đấu giá của chính mình!");
        }

        if (!checkDeposit(customer.getCustomerId(), auction.getAuctionId())) {
            throw new InvalidActionException("Bạn cần hoàn tất đặt cọc để tham gia đấu giá!");
        }

        validateBidAmount(auction, bidDTO.getBidAmount());

        resetOldBids(auction);

        Bid newBid = new Bid();
        newBid.setAuction(auction);
        newBid.setCustomer(customer);
        newBid.setBidAmount(bidDTO.getBidAmount());
        newBid.setBidTime(LocalDateTime.now());
        newBid.setIsWinner(Boolean.TRUE); // ✅ Gán isWinner là Boolean.TRUE thay vì true

        Bid savedBid = bidRepository.save(newBid);

        return mapToBidResponseDTO(savedBid);
    }

    // ---------------------- HISTORY & WINNER ----------------------

    public List<BidResponseDTO> getBidHistoryByAuctionId(Integer auctionId) {
        List<Bid> bids = bidRepository.findByAuction_AuctionIdOrderByBidAmountDesc(auctionId);
        if (bids == null || bids.isEmpty()) {
            return new ArrayList<>();
        }

        return bids.stream().map(bid -> BidResponseDTO.builder()
                        .bidId(bid.getBidId())
                        .auctionId(bid.getAuction().getAuctionId())
                        .customerId(bid.getCustomer().getCustomerId())
                        .bidAmount(bid.getBidAmount())
                        .bidTime(bid.getBidTime())
                        .isWinner(bid.getIsWinner()) // ✅ Trả về Boolean thay vì Integer
                        .message("Lịch sử đấu giá")
                        .user(bid.getAccount())
                        .build())
                .collect(Collectors.toList());
    }

    public Optional<Bid> getCurrentHighestBid(Integer auctionId) {
        return bidRepository.findTopByAuctionOrderByBidAmountDesc(auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá!")));
    }

    public Integer getCustomerIdFromUsername(String username) {
        Customer customer = customerRepository.findByAccountUsername(username)
                .orElseThrow(() -> new CustomerNotFoundException("Không tìm thấy tài khoản khách hàng!"));
        return customer.getCustomerId();
    }

    // ---------------------- SUPPORT METHODS ----------------------
    private void validateAuctionStatus(Auction auction) {
        if (!auction.getStatus().equals(Auction.AuctionStatus.active)) {
            throw new InvalidActionException("Phiên đấu giá không còn hoạt động!");
        }
        if (auction.getAuctionEndTime().isBefore(LocalDateTime.now())) {
            throw new InvalidActionException("Phiên đấu giá đã kết thúc!");
        }
    }

    private void validateBidAmount(Auction auction, BigDecimal bidAmount) {
        BigDecimal minNextBid = auction.getCurrentPrice().add(auction.getBidStep());
        if (bidAmount.compareTo(minNextBid) < 0) {
            throw new InvalidActionException("Giá đấu phải tối thiểu từ " + minNextBid + " trở lên!");
        }
    }

    private void resetOldBids(Auction auction) {
        List<Bid> oldBids = bidRepository.findByAuction(auction);
        oldBids.forEach(b -> {
            b.setIsWinner(Boolean.FALSE); // ✅ Sử dụng Boolean.FALSE thay vì false
            System.out.println("Reset isWinner: " + b.getBidId() + " -> false");
        });
        bidRepository.saveAll(oldBids);
    }

    private BidResponseDTO mapToBidResponseDTO(Bid bid) {
        System.out.println("Bid ID: " + bid.getBidId() + " | isWinner: " + bid.getIsWinner());
        return new BidResponseDTO(
                bid.getBidId(),
                bid.getAuction().getAuctionId(),
                bid.getCustomer().getCustomerId(),
                bid.getBidAmount(),
                bid.getBidTime(),
                bid.getIsWinner(), // ✅ Trả về Boolean thay vì Integer
                "Đặt giá thành công!",
                bid.getAccount()
        );
    }

    /**
     * Kiểm tra xem người dùng đã thanh toán đặt cọc hay chưa
     */
    public boolean checkDeposit(Integer customerId, Integer auctionId) {
        return transactionRepository.existsByCustomer_CustomerIdAndAuction_AuctionIdAndTransactionType(customerId, auctionId, "DEPOSIT");
    }

    /**
     * Lưu thông tin giao dịch đặt cọc
     */
    public void saveDepositTransaction(Integer customerId, Integer auctionId, Double amount, String method) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng!"));

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá!"));

        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setAuction(auction);
        transaction.setAmount(amount);
        transaction.setPaymentMethod(method);
        transaction.setTransactionType("DEPOSIT");
        transaction.setStatus("COMPLETED");
        transaction.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);
    }

    @Scheduled(fixedRate = 60000) // Mỗi 60 giây
    public void updateAuctionStatuses() {
        auctionRepository.updateAuctionStatuses(LocalDateTime.now());
    }
}
