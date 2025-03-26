package com.example.auction_management.service;

import com.example.auction_management.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public interface ICustomerService extends IService<Customer, Integer> {
    Page<Customer> getCustomers(Pageable pageable);
    Customer getCustomerByUsername(String username);
    Customer getCustomerById(Integer id);
    Integer getCustomerIdByUsername(String username);
}
