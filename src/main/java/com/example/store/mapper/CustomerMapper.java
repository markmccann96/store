package com.example.store.mapper;

import com.example.store.entity.Customer;
import com.example.store.api.dto.CustomerDTO;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerDTO customerToCustomerDTO(Customer customer);

    List<CustomerDTO> customersToCustomerDTOs(List<Customer> customer);
}
