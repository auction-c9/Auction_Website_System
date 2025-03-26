package com.example.auction_management.repository;

import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Follow;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow,Integer> {
    boolean existsByFollowerAndSeller(Customer follower, Customer seller);

    // Thêm @Transactional và @Modifying cho delete
    @Transactional
    @Modifying
    @Query("DELETE FROM Follow f WHERE f.follower = :follower AND f.seller = :seller")
    void deleteByFollowerAndSeller(@Param("follower") Customer follower,
                                   @Param("seller") Customer seller);

    @Query("SELECT f.follower FROM Follow f WHERE f.seller = :seller")
    List<Customer> findFollowersBySeller(@Param("seller") Customer seller);

}
