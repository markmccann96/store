package com.example.store.controller;

import com.example.store.api.dto.OrderDTO;
import com.example.store.controller.api.OrderApi;
import com.example.store.entity.Order;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.OrderRepository;

import com.example.store.service.OrderQueryService;
import com.example.store.service.SnapshotTagService;
import com.example.store.utils.ResponseUtility;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final SnapshotTagService snapshotTagService;
    private final OrderQueryService orderQueryService;

    @Override
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrders(@Parameter Integer limit, @Parameter Integer offset) {
        if ( limit == null ) {
            SnapshotTagService.Snapshot current = snapshotTagService.current();
           HttpServletRequest request = currentRequest();
           String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
           var ifModifiedSince = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);

           if (snapshotTagService.matchesConditional(ifNoneMatch, ifModifiedSince, current)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(current.etag())
                    .lastModified(current.lastModified())
                    .build();
           }


            List<OrderDTO> all = orderQueryService.findAllOrdersWithProducts();
            return ResponseEntity.ok()
                    .eTag(current.etag())
                    .lastModified(current.lastModified())
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "ETag, Last-Modified")
                    .body(all);

        }
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit);
        Page<Order> orders = orderRepository.findAll(pageable);
        List<OrderDTO> orderDTOs = orderMapper.ordersToOrderDTOs(orders.getContent());
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(orders.getTotalElements()));
        String links = ResponseUtility.buildLinkHeader(limit, offset, orders.getTotalElements());
        headers.add(HttpHeaders.LOCATION, links);
        return new ResponseEntity<>(orderDTOs, headers, HttpStatus.OK);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO orderToCreate) {
        Order entity = orderMapper.orderDTOToOrder(orderToCreate);
        entity.setId(null);
        Order savedOrder = orderRepository.save(entity);
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

    private static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return ((ServletRequestAttributes) attrs).getRequest();
    }
}
