package com.example.auction_management.service;

import com.example.auction_management.model.Customer;

public interface IFollowService {
    void followSeller(Integer sellerId, Customer follower);
    void unfollowSeller(Integer sellerId, Customer follower);
    boolean checkFollowStatus(Integer sellerId, Customer follower);
}
