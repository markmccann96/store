package com.example.store.controller;

import com.example.store.api.dto.OrderCustomerDTO;
import com.example.store.api.dto.OrderDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.repository.projection.OrderRow;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.projection.ProductSummaryView;
import com.example.store.service.OrderQueryService;
import com.example.store.service.SnapshotTagService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@ComponentScan(basePackageClasses = {CustomerMapper.class})
@RequiredArgsConstructor
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private SnapshotTagService snapshotTagService;

    @MockitoBean
    private OrderQueryService orderQueryService;

    private Order order;
    private Customer customer;
    private OrderRow row;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setName("John Doe");
        customer.setId(1L);

        order = new Order();
        order.setDescription("Test Order");
        order.setId(1L);
        order.setCustomer(customer);

        row = new OrderRow() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public String getDescription() {
                return "Test Order";
            }

            @Override
            public Long getCustomerId() {
                return 1L;
            }

            @Override
            public String getCustomerName() {
                return "John Doe";
            }

            @Override
            public List<ProductSummaryView> getProducts() {
                return List.of();
            }
        };
    }

    @Test
    void testCreateOrder() throws Exception {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setDescription(order.getDescription());
        savedOrder.setProducts(order.getProducts());
        savedOrder.setCustomer(order.getCustomer());
        when(orderRepository.save(any())).thenReturn(savedOrder);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.name").value("John Doe"));
    }

    @Test
    void testGetOrder() throws Exception {
        when(orderRepository.findAllRows()).thenReturn(List.of(row));
        when(snapshotTagService.current()).thenReturn(new SnapshotTagService.Snapshot("aTag", 1000));
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setCustomer(new OrderCustomerDTO());
        orderDTO.getCustomer().setName("John Doe");
        orderDTO.setDescription("Test Order");
        when(orderQueryService.findAllOrdersWithProducts()).thenReturn(List.of(orderDTO));

        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..description").value("Test Order"))
                .andExpect(jsonPath("$..customer.name").value("John Doe"));
    }

    @Test
    void testGetOrderById() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.name").value("John Doe"));
    }

    @Test
    void testGetOrderByIdNotFound() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/order/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns304WhenUnchanged() throws Exception {
        // first call -> 200 with ETag
        when(orderRepository.findAllRows()).thenReturn(List.of(row));
        when(snapshotTagService.current()).thenReturn(new SnapshotTagService.Snapshot("aTag", 1000));
        var res1 = mockMvc.perform(get("/order")).andReturn().getResponse();
        var etag = res1.getHeader("ETag");
        var lastMod = res1.getHeader("Last-Modified");

        Mockito.clearInvocations(snapshotTagService, orderRepository);
        SnapshotTagService.Snapshot snapshot2 = new SnapshotTagService.Snapshot("aTag", 1000);
        when(snapshotTagService.current()).thenReturn(snapshot2);
        when(orderRepository.findAllRows()).thenReturn(List.of(row));
        when(snapshotTagService.matchesConditional("aTag", 1000, snapshot2)).thenReturn(true);

        // second call with validators -> 304
        mockMvc.perform(get("/order")
                        .header("If-None-Match", etag)
                        .header("If-Modified-Since", lastMod))
                .andExpect(status().isNotModified());
    }

    @Test
    void returns200WhenChanged() throws Exception {

        when(snapshotTagService.current()).thenReturn(new SnapshotTagService.Snapshot("aTag", 1000));
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setDescription("Test Order");
        orderDTO.setCustomer(new OrderCustomerDTO());
        orderDTO.getCustomer().setName("Joe Doe");
        when(orderQueryService.findAllOrdersWithProducts()).thenReturn(List.of(orderDTO));
        var res1 = mockMvc.perform(get("/order")).andReturn().getResponse();
        var etag = res1.getHeader("ETag");

        Mockito.clearInvocations(snapshotTagService, orderRepository, orderQueryService);
        SnapshotTagService.Snapshot snapshot2 = new SnapshotTagService.Snapshot("eTag2", 1000);
        when(snapshotTagService.current()).thenReturn(snapshot2);
        when(snapshotTagService.matchesConditional("aTag", 1000, snapshot2)).thenReturn(false);
        mockMvc.perform(get("/order").header("If-None-Match", etag))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(orderQueryService).findAllOrdersWithProducts();
    }
}
