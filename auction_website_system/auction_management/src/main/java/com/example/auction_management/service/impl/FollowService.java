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
    private final CustomerService customerService;

    @Transactional
    public void followSeller(Integer sellerId, Customer follower) {
        Customer seller = getCustomerById(sellerId);
        if (followRepository.existsByFollowerAndSeller(follower, seller)) {
            throw new IllegalStateException("Đã theo dõi người bán này");
        }
        validateFollowRelationship(follower, seller);

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setSeller(seller);
        followRepository.save(follow);
    }

    @Transactional
    public void unfollowSeller(Integer sellerId, Customer follower) {
        Customer seller = getCustomerById(sellerId);
        followRepository.deleteByFollowerAndSeller(follower, seller);
    }

    public boolean checkFollowStatus(Integer sellerId, Customer follower) {
        Customer seller = getCustomerById(sellerId);
        return followRepository.existsByFollowerAndSeller(follower, seller);
    }

    private Customer getCustomerById(Integer id) {
        return customerService.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    private void validateFollowRelationship(Customer follower, Customer seller) {
        if (followRepository.existsByFollowerAndSeller(follower, seller)) {
            throw new IllegalArgumentException("Đã theo dõi người bán này");
        }
    }
}
