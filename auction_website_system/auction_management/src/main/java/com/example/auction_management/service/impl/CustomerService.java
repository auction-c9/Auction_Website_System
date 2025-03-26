package com.example.auction_management.service.impl;

import com.example.auction_management.exception.NotFoundException;
import com.example.auction_management.model.Customer;
import com.example.auction_management.repository.CustomerRepository;
import com.example.auction_management.repository.ImageRepository;
import com.example.auction_management.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
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
    public Page<Customer> getCustomers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return customerRepository.findAllNonAdminCustomers(pageable);
    }


    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public Optional<Customer> findById(Integer integer) {
        return customerRepository.findById(integer);
    }

    @Override
    public Customer save(Customer entity) {
        return null;
    }

    @Override
    public void deleteById(Integer integer) {

    }

    @Override
    public Customer getCustomerByUsername(String username) {
        Integer customerId = customerRepository.findCustomerIdByUsername(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Customer với username: " + username));
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer không tồn tại"));
    }

    @Override
    public Customer getCustomerById(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    @Override
    public Integer getCustomerIdByUsername(String username) {
        return customerRepository.findCustomerIdByUsername(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy customer với username: " + username));
    }
}
