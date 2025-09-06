package com.example.store.service;


import com.example.store.api.dto.OrderDTO;
import com.example.store.api.dto.ProductSummaryDTO;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.projection.OrderRow;
import com.example.store.repository.projection.ProductSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public List<OrderDTO> findAllOrdersWithProducts() {
        // load the basic order/client information as a projection
        List<OrderRow> orders = orderRepository.findAllRows();
        if (orders.isEmpty()) return List.of();

        // load all the product / order mappings as a projection
        List<ProductSummaryView> prows = orderRepository.findAllOrderProducts();

        // Create a map of order ids to product summary information
        Map<Long, List<ProductSummaryDTO>> productsByOrder =
                prows.stream()
                        .collect(Collectors.groupingBy(
                                ProductSummaryView::getOrderId,
                                Collectors.mapping(orderMapper::toProductSummaryDto, Collectors.toList())
                        ));

        // Stitch together the map and list into a single order id
        List<OrderDTO> result = new ArrayList<>(orders.size());
        for (OrderRow r : orders) {
            OrderDTO dto = orderMapper.rowToDto(r);
            dto.setProducts(productsByOrder.getOrDefault(r.getId(), List.of()));
            result.add(dto);
        }
        return result;
    }
}

