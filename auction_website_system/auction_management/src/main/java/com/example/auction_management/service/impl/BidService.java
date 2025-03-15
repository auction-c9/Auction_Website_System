package com.example.auction_management.service.impl;

import com.example.auction_management.dto.BidDTO;
import com.example.auction_management.dto.BidResponseDTO;
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
    public BidResponseDTO placeBid(BidDTO bidDTO) {
        // Lấy thông tin phiên đấu giá theo ID
        Auction auction = auctionRepository.findById(bidDTO.getAuctionId())
                .orElseThrow(() -> new AuctionNotFoundException("Phiên đấu giá không tồn tại!"));

        // Lấy thông tin khách hàng theo ID
        Customer customer = customerRepository.findById(bidDTO.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Khách hàng không tồn tại!"));

        // Kiểm tra phiên đấu giá còn hoạt động
        if (!auction.getStatus().equals(Auction.AuctionStatus.active)) {
            throw new AuctionNotActiveException("Phiên đấu giá này không còn hoạt động!");
        }

        // Kiểm tra thời gian phiên đấu giá
        LocalDateTime now = LocalDateTime.now();
        if (auction.getAuctionEndTime().isBefore(now)) {
            throw new AuctionEndedException("Phiên đấu giá đã kết thúc!");
        }

        // Kiểm tra giá thầu có hợp lệ không
        // Lưu ý: Mặc dù chúng ta không cập nhật currentPrice trong Auction,
        // chúng ta vẫn dùng giá hiện tại của phiên đấu giá để tính giá thầu tối thiểu
        BigDecimal minNextBid = auction.getCurrentPrice().add(auction.getBidStep());
        if (bidDTO.getBidAmount().compareTo(minNextBid) < 0) {
            throw new BidAmountTooLowException("Giá đấu phải từ " + minNextBid + " trở lên!");
        }

        // Reset trạng thái các bid cũ về isWinner = false
        List<Bid> auctionBids = bidRepository.findByAuction(auction);
        auctionBids.forEach(b -> b.setIsWinner(false));
        bidRepository.saveAll(auctionBids);

        // Không cập nhật giá hiện tại của phiên đấu giá
        // auction.setCurrentPrice(bidDTO.getBidAmount());
        // auctionRepository.save(auction);

        // Tạo bid mới với giá đấu (bidAmount) từ request
        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setCustomer(customer);
        bid.setBidAmount(bidDTO.getBidAmount());
        bid.setIsWinner(true);
        bid.setBidTime(now);

        Bid savedBid = bidRepository.save(bid);

        // Tạo DTO trả về
        return new BidResponseDTO(
                savedBid.getBidId(),
                auction.getAuctionId(),
                customer.getCustomerId(),
                savedBid.getBidAmount(),
                savedBid.getBidTime(),
                savedBid.getIsWinner()
        );
    }



    // --- Custom Exceptions ---
    public static class AuctionNotFoundException extends RuntimeException {
        public AuctionNotFoundException(String message) { super(message); }
    }

    public static class CustomerNotFoundException extends RuntimeException {
        public CustomerNotFoundException(String message) { super(message); }
    }

    public static class AuctionNotActiveException extends RuntimeException {
        public AuctionNotActiveException(String message) { super(message); }
    }

    public static class AuctionEndedException extends RuntimeException {
        public AuctionEndedException(String message) { super(message); }
    }

    public static class BidAmountTooLowException extends RuntimeException {
        public BidAmountTooLowException(String message) { super(message); }
    }
}
