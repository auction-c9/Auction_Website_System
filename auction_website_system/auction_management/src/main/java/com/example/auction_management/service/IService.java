package com.example.auction_management.service;

import java.util.List;
import java.util.Optional;

public interface IService<T> {
    List<T> findAll();
    Optional<T> findById(Integer id);
    T save(T t);
    void deleteById(Integer id);
}
