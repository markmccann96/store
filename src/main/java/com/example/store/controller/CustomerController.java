package com.example.store.controller;

import com.example.store.controller.api.CustomerApi;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.api.dto.CustomerDTO;

import com.example.store.utils.ResponseUtility;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController implements CustomerApi {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;


    @Override
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getCustomers(@Parameter Integer limit, @Parameter Integer offset) {

        if ( limit == null || limit == 0 ) {
            // for backward compatability we return all customers.
            return ResponseEntity.ok(customerMapper.customersToCustomerDTOs(customerRepository.findAll()));
        }
        int page = offset / limit; // assumes that the offset is the total number of records to offset.
        Pageable pageable = PageRequest.of(page, limit);
        Page<Customer> customers = customerRepository.findAll(pageable);
        HttpHeaders headers = getPageableHeaders(limit, offset, customers);
        return new ResponseEntity<>(customerMapper.customersToCustomerDTOs(customers.getContent()), headers, HttpStatus.OK);
    }

    private static HttpHeaders getPageableHeaders(Integer limit, Integer offset, Page<Customer> customers) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(customers.getTotalElements()));
        String links = ResponseUtility.buildLinkHeader(limit, offset, customers.getTotalPages());
        headers.add(HttpHeaders.LINK, links);
        return headers;
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<List<CustomerDTO>> searchCustomers( @Parameter String name, Integer limit, Integer offset) {
        if ( limit == null || limit == 0 ) {
            return ResponseEntity.ok(customerMapper.customersToCustomerDTOs(customerRepository.findByNameIgnoreCaseContaining(name)));
        }

        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit);
        Page<Customer> customers = customerRepository.findByNameIgnoreCaseContaining(name, pageable);
        HttpHeaders headers = getPageableHeaders(limit, offset, customers);
        return new ResponseEntity<>(customerMapper.customersToCustomerDTOs(customers.getContent()), headers, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> createCustomer(CustomerDTO customerDTO) {

        return CustomerApi.super.createCustomer(customerDTO);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDTO createCustomer(@RequestBody Customer customer) {
        return customerMapper.customerToCustomerDTO(customerRepository.save(customer));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(customerMapper.customerToCustomerDTO(customer));
    }
}
