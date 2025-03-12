package com.example.auction_management.repository;

import com.example.auction_management.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Integer> {

    @Query("SELECT r FROM role r WHERE r.name = :name")
    Optional<Role> findByName(@Param("name") String name);
}
