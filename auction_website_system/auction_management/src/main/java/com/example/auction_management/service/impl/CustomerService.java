package com.example.auction_management.service.impl;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Image;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.ImageRepository;
import com.example.auction_management.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService implements ICustomerService {
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Override
    public Optional<Customer> findByAccountUsername(String username) {
        return Optional.empty();
    }

    @Override
    public Page<Customer> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
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
}
