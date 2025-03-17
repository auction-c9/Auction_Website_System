package com.example.auction_management.service;

import com.example.auction_management.model.Customer;

import java.util.Optional;

public interface ICustomerService extends IService<Customer, Integer> {
    Optional<Customer> findByAccountUsername(String username);
}
