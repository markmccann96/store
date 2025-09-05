package com.example.store.repository;

import com.example.store.entity.Customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface CustomerRepository extends PagingAndSortingRepository<Customer, Long>, JpaRepository<Customer, Long> {

    List<Customer> findByNameIgnoreCaseContaining(String name);

    Page<Customer> findByNameIgnoreCaseContaining(String name, Pageable pageable);
}
