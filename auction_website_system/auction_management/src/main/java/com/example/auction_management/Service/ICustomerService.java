package com.example.auction_management.Service;

import com.example.auction_management.model.Customer;

import java.math.BigDecimal;

public interface ICustomerService {
    void updateUserBalance(Integer userId, BigDecimal amount);
    Customer findCustomerByUsername(String username);

    void updateCustomer(Customer customer);
    Customer save(Customer customer);

}
