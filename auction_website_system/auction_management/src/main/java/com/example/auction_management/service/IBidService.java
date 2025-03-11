package com.example.auction_management.service;

import com.example.auction_management.dto.BidDTO;
import com.example.auction_management.model.Bid;

/**
 * Interface dành riêng cho các nghiệp vụ liên quan đến đấu giá (Bid).
 */
public interface IBidService extends IService<Bid, Integer> {

    /**
     * Thực hiện đấu giá mới.
     *
     * @param bidDTO Thông tin đấu giá.
     * @return Đối tượng Bid đã được lưu.
     * @throws Exception Nếu xảy ra lỗi khi đấu giá.
     */
    Bid placeBid(BidDTO bidDTO) throws Exception;
}
