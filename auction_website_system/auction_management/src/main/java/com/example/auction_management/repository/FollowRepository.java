package com.example.auction_management.repository;

import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow,Integer> {
    boolean existsByFollowerAndSeller(Customer follower, Customer seller);
    void deleteByFollowerAndSeller(Customer follower, Customer seller);

    @Query("SELECT f FROM Follow f WHERE f.seller = :seller")
    List<Follow> findBySeller(@Param("seller") Customer seller);
}
