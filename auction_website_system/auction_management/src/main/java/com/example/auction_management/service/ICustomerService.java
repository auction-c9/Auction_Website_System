package com.example.auction_management.service;

import com.example.auction_management.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ICustomerService extends IService<Customer, Integer> {
    Page<Customer> getCustomers(int page, int size);
}
