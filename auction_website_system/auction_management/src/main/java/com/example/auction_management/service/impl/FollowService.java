package com.example.auction_management.service.impl;

import com.example.auction_management.exception.NotFoundException;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Follow;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.FollowRepository;
import com.example.auction_management.service.IFollowService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FollowService implements IFollowService {
    private final FollowRepository followRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public void followSeller(Integer sellerAccountId, Customer follower) {
        Customer seller = customerRepository.findByAccount_AccountId(sellerAccountId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người bán"));

        if (follower.getCustomerId().equals(sellerAccountId)) {
            throw new IllegalArgumentException("Không thể theo dõi chính bạn");
        }

        if (followRepository.existsByFollowerAndSeller(follower, seller)) {
            throw new IllegalStateException("Đã theo dõi người bán này");
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setSeller(seller);
        followRepository.save(follow);
    }

    @Transactional
    public void unfollowSeller(Integer sellerAccountId, Customer follower) {
        // Sửa thành tìm seller bằng account_id (giống followSeller)
        Customer seller = customerRepository.findByAccount_AccountId(sellerAccountId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người bán"));

        // Kiểm tra tồn tại trước khi xóa
        if (!followRepository.existsByFollowerAndSeller(follower, seller)) {
            throw new NotFoundException("Bạn chưa theo dõi người bán này");
        }

        followRepository.deleteByFollowerAndSeller(follower, seller);
    }

    public boolean checkFollowStatus(Integer sellerAccountId, Customer follower) {
        Customer seller = customerRepository.findByAccount_AccountId(sellerAccountId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người bán"));

        return followRepository.existsByFollowerAndSeller(follower, seller);
    }
}
