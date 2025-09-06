package com.example.store.controller;

import com.example.store.controller.api.CustomerApi;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.api.dto.CustomerDTO;

import com.example.store.utils.ResponseUtility;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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
    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(CustomerDTO customerDTO) {
        Customer newCustomer = customerMapper.toEntity(customerDTO);
        newCustomer.setId(null);

         if (newCustomer.getOrders() != null) {
            newCustomer.getOrders().forEach(o -> {
                o.setId(null);
                o.setCustomer(newCustomer);
            });
        }

        try {
            Customer savedCustomer = customerRepository.save(newCustomer);
            CustomerDTO dto = customerMapper.customerToCustomerDTO(savedCustomer);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequestUri()
                    .path("/{id}")
                    .buildAndExpand(savedCustomer.getId())
                    .toUri();

            return ResponseEntity.created(location)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).build();
        }
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
        if ( id == null ) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(customerMapper.customerToCustomerDTO(customer));
    }
}
