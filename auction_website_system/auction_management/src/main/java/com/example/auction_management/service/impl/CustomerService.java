package com.example.auction_management.service.impl;

import com.example.auction_management.model.Account;
import com.example.auction_management.model.Customer;
import com.example.auction_management.model.Image;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.ImageRepository;
import com.example.auction_management.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService implements ICustomerService {
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ImageRepository imageRepository;

}
