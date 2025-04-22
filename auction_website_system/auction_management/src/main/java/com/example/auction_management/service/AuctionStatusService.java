package com.example.auction_management.service;

import com.example.auction_management.model.Auction;
import com.example.auction_management.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionStatusService {

    private final AuctionRepository auctionRepository;
    private final NotificationService notificationService;

    /**
     * Cập nhật trạng thái các phiên đấu giá dựa trên thời gian hiện tại
     * và gửi thông báo cho những phiên đấu giá kết thúc mà chưa được thông báo.
     * Phương thức này sẽ chạy tự động theo scheduler (mỗi 1 giây).
     */
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void updateAuctionStatusesAndNotify() {
        // Cập nhật trạng thái của các phiên đấu giá
        auctionRepository.updateAuctionStatuses(LocalDateTime.now());

        // Lấy danh sách các phiên đấu giá đã kết thúc nhưng chưa được thông báo
        List<Auction> endedAuctions = auctionRepository.findByAuctionEndTimeBeforeAndWinnerNotifiedFalse(LocalDateTime.now());
        for (Auction auction : endedAuctions) {
            // Nếu phiên đấu giá kết thúc quá 2 phút, bỏ qua hoặc có thể thêm logic xử lý khác
            if (auction.getAuctionEndTime().plusMinutes(2).isBefore(LocalDateTime.now())) {
                continue;
            }
            // Gửi thông báo và email thông qua NotificationService
            notificationService.sendAuctionEndCommunications(auction);

            // Đánh dấu phiên đấu giá đã được thông báo
            auction.setWinnerNotified(true);
            auctionRepository.save(auction);
        }
    }
}
