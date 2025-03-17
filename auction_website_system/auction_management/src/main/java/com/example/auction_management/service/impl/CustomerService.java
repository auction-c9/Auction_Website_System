package com.example.auction_management.service.impl;

import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Role;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService implements ICustomerService {
    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public List<Customer> findAll() {
        return List.of();
    }

    @Override
    public Optional<Customer> findById(Integer integer) {
        return Optional.empty();
    }

    @Override
    public Customer save(Customer entity) {
        return null;
    }

    @Override
    public void deleteById(Integer integer) {

    }

    @Override
    public Optional<Customer> findByAccountUsername(String username) {
        return customerRepository.findByAccountUsername(username);
    }
}
