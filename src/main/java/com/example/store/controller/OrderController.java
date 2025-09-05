package com.example.store.controller;

import com.example.store.api.dto.OrderDTO;
import com.example.store.controller.api.OrderApi;
import com.example.store.entity.Order;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrders() {
        return ResponseEntity.ok(orderMapper.ordersToOrderDTOs(orderRepository.findAll()));
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO orderToCreate) {
        Order savedOrder = orderRepository.save(orderMapper.orderDTOToOrder(orderToCreate));
        OrderDTO orderDTO = orderMapper.orderToOrderDTO(savedOrder);
        URI createdURI = URI.create("/order/" + orderDTO.getId());
        return ResponseEntity.created(createdURI).body(orderDTO);
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById( @PathVariable Long id) {
        Order anOrder = orderRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(orderMapper.orderToOrderDTO(anOrder));
    }
}
