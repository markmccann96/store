package com.example.store.service;

import com.example.store.api.dto.OrderDTO;
import com.example.store.api.dto.ProductSummaryDTO;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.projection.OrderRow;
import com.example.store.repository.projection.ProductSummaryView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderQueryService.findAllOrdersWithProducts()
 */
@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    OrderRepository orderRepository;

    // Use the real MapStruct mapper (requires mapstruct processor on test compile classpath)
    OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

    OrderQueryService service;

    @BeforeEach
    void setUp() {
        service = new OrderQueryService(orderRepository, orderMapper);
    }

    @Test
    void returnsEmptyListWhenNoOrders() {
        when(orderRepository.findAllRows()).thenReturn(List.of());

        List<OrderDTO> result = service.findAllOrdersWithProducts();

        assertThat(result).isEmpty();
        verify(orderRepository).findAllRows();
        verify(orderRepository, never()).findAllOrderProducts();
    }

    @Test
    void stitchesProductsForEachOrder_preservesOrder() {
        // Orders (lean projection)
        var o1 = new OrderRowImpl(1L, "Order A", 10L, "Acme", new ArrayList<>());
        var o2 = new OrderRowImpl(2L, "Order B", 10L, "Acme", new ArrayList<>());
        when(orderRepository.findAllRows()).thenReturn(List.of(o1, o2));

        // Single pass over join table → (orderId, productId, description)
        var r1 = new ProductSummaryViewImpl(1L, 100L, "Widget");
        var r2 = new ProductSummaryViewImpl(1L, 200L, "Gadget");
        var r3 = new ProductSummaryViewImpl(2L, 200L, "Gadget");
        when(orderRepository.findAllOrderProducts()).thenReturn(List.of(r1, r2, r3));

        List<OrderDTO> result = service.findAllOrdersWithProducts();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getCustomer().getId()).isEqualTo(10L);
        assertThat(result.get(0).getCustomer().getName()).isEqualTo("Acme");
        assertThat(result.get(0).getProducts())
                .extracting(ProductSummaryDTO::getId, ProductSummaryDTO::getDescription)
                .containsExactlyInAnyOrder(
                        tuple2(100L, "Widget"),
                        tuple2(200L, "Gadget")
                );

        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getProducts())
                .extracting(ProductSummaryDTO::getId, ProductSummaryDTO::getDescription)
                .containsExactly(tuple2(200L, "Gadget"));

        verify(orderRepository).findAllRows();
        verify(orderRepository).findAllOrderProducts();
    }

    @Test
    void orderWithNoProducts_yieldsEmptyProductsList() {
        var o3 = new OrderRowImpl(3L, "Order C", 11L, "Beta", new ArrayList<>());
        when(orderRepository.findAllRows()).thenReturn(List.of(o3));
        when(orderRepository.findAllOrderProducts()).thenReturn(List.of()); // none

        List<OrderDTO> result = service.findAllOrdersWithProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(3L);
        assertThat(result.get(0).getProducts()).isEmpty();
    }

    // --- helpers -------------------------------------------------------------

    /**
     * Compact tuple helper for AssertJ extraction.
     */
    private static Tuple tuple2(Object a, Object b) {
        return new Tuple(a, b);
    }

    /**
     * Simple test impl of the lean order projection.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class OrderRowImpl implements OrderRow {
        private Long id;
        private String description;
        private Long customerId;
        private String customerName;
        private List<ProductSummaryView> products;
    }


    /**
     * Simple test impl of the (order, product) join projection.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class ProductSummaryViewImpl implements ProductSummaryView {
        Long orderId;
        Long productId;
        String productDescription;
    }
}
