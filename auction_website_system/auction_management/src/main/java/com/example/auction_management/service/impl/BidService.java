package com.example.auction_management.service.impl;

import com.example.auction_management.dto.BidDTO;
import com.example.auction_management.model.Auction;
import com.example.auction_management.model.Bid;
import com.example.auction_management.model.Customer;
import com.example.auction_management.repository.AuctionRepository;
import com.example.auction_management.repository.BidRepository;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.service.IBidService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BidService implements IBidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final CustomerRepository customerRepository;

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
        bidRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Bid placeBid(BidDTO bidDTO) {
        // Lấy phiên đấu giá theo ID
        Auction auction = auctionRepository.findById(bidDTO.getAuctionId())
                .orElseThrow(() -> new AuctionNotFoundException("Phiên đấu giá không tồn tại!"));
        // Lấy khách hàng theo ID
        Customer customer = customerRepository.findById(bidDTO.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Khách hàng không tồn tại!"));

        // Kiểm tra trạng thái phiên đấu giá
        if (!auction.getStatus().equals(Auction.AuctionStatus.active)) {
            throw new AuctionNotActiveException("Phiên đấu giá này không còn hoạt động!");
        }

        // Kiểm tra thời gian phiên đấu giá
        LocalDateTime now = LocalDateTime.now();
        if (auction.getAuctionEndTime().isBefore(now)) {
            throw new AuctionEndedException("Phiên đấu giá đã kết thúc!");
        }

        // Kiểm tra giá thầu có hợp lệ hay không
        BigDecimal minNextBid = auction.getCurrentPrice().add(auction.getBidStep());
        if (bidDTO.getBidAmount().compareTo(minNextBid) < 0) {
            throw new BidAmountTooLowException("Giá đấu phải tối thiểu: " + minNextBid);
        }

        // Cập nhật giá hiện tại của phiên đấu giá
        auction.setCurrentPrice(bidDTO.getBidAmount());
        auctionRepository.save(auction);

        // Reset trạng thái các bid cũ
        List<Bid> auctionBids = bidRepository.findByAuction(auction);
        for (Bid b : auctionBids) {
            b.setIsWinner(false);
        }
        bidRepository.saveAll(auctionBids);

        // Tạo và lưu bid mới
        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setCustomer(customer);
        bid.setBidAmount(bidDTO.getBidAmount());
        bid.setIsWinner(true);
        return bidRepository.save(bid);
    }

    // --- Custom Exceptions cho nghiệp vụ đấu giá ---
    public static class AuctionNotFoundException extends RuntimeException {
        public AuctionNotFoundException(String message) {
            super(message);
        }
    }

    public static class CustomerNotFoundException extends RuntimeException {
        public CustomerNotFoundException(String message) {
            super(message);
        }
    }

    public static class AuctionNotActiveException extends RuntimeException {
        public AuctionNotActiveException(String message) {
            super(message);
        }
    }

    public static class AuctionEndedException extends RuntimeException {
        public AuctionEndedException(String message) {
            super(message);
        }
    }

    public static class BidAmountTooLowException extends RuntimeException {
        public BidAmountTooLowException(String message) {
            super(message);
        }
    }
}
