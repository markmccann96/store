package com.example.store.repository;

import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.entity.Customer; // adjust if your package differs
import com.example.store.repository.projection.OrderRow;
import com.example.store.repository.projection.ProductSummaryView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderProductRepositoryTest {

    @Autowired EntityManager em;
    @Autowired OrderRepository orderRepository;
    @Autowired ProductRepository productRepository;

    Long order1Id;
    Long order2Id;
    Long p1Id ;
    Long p2Id;

    @BeforeEach
    void setup() {
        // Customer (minimal)
        var cust = new Customer();
        // If your Customer id is generated, leave null; else set explicitly
        cust.setName("Acme Co");
        em.persist(cust);

        // Products (IDs are client-supplied per Liquibase/OpenAPI)
        var p1 = Product.builder().description("Widget").build();
        var p2 = Product.builder().description("Gadget").build();
        em.persist(p1);
        em.persist(p2);
        em.flush();
        em.clear();

        p1Id = p1.getId();
        p2Id = p2.getId();

        // Orders (assume generated IDs)
        var o1 = new Order();
        o1.setDescription("Order A");
        o1.setCustomer(cust);
        // createdAt is optional; set if your entity has it
        try { o1.getClass().getMethod("setCreatedAt", Instant.class); o1.setCreatedAt(Instant.now()); } catch (Exception ignore) {}

        Product p1Ref = em.getReference(Product.class, p1.getId());
        Product p2Ref = em.getReference(Product.class, p2.getId());
        // link products (both sides)
        o1.getProducts().add(p1Ref); p1Ref.getOrders().add(o1);
        o1.getProducts().add(p2Ref); p2Ref.getOrders().add(o1);
        em.persist(o1);

        var o2 = new Order();
        o2.setDescription("Order B");
        o2.setCustomer(cust);
        try { o2.getClass().getMethod("setCreatedAt", Instant.class); o2.setCreatedAt(Instant.now()); } catch (Exception ignore) {}
        o2.getProducts().add(p2Ref); p2Ref.getOrders().add(o2);
        em.persist(o2);

        em.flush();
        em.clear();

        order1Id = o1.getId();
        order2Id = o2.getId();
        assertThat(order1Id).isNotNull();
        assertThat(order2Id).isNotNull();
    }

    @Test
    @DisplayName("ProductRepository.findOrderIdsByProductId returns correct order IDs")
    void productOrderIds() {
        var p1Orders = productRepository.findOrderIdsByProductId(p1Id);
        var p2Orders = productRepository.findOrderIdsByProductId(p2Id);

        assertThat(p1Orders).containsExactly(order1Id);                 // p1 only in o1
        assertThat(p2Orders).containsExactlyInAnyOrder(order1Id, order2Id); // p2 in both
    }

    @Test
    @DisplayName("ProductRepository.findWithOrdersById eagerly loads orders")
    void productWithOrders() {
        var opt = productRepository.findWithOrdersById(p2Id);
        assertThat(opt).isPresent();
        var p2Loaded = opt.get();
        // Within @DataJpaTest transaction, LAZY is fine, but EntityGraph should prefetch anyway
        assertThat(p2Loaded.getOrders().stream().map(Order::getId).toList())
                .containsExactlyInAnyOrder(order1Id, order2Id);
    }

    @Test
    @DisplayName("OrderRepository.findAllRows returns lean rows")
    void orderRowsLean() {
        List<OrderRow> rows = orderRepository.findAllRows();
        assertThat(rows).hasSize(2);

        var ids = rows.stream().map(OrderRow::getId).toList();
        assertThat(ids).containsExactlyInAnyOrder(order1Id, order2Id);

        // Assert flattened customer fields are present
        var row = rows.get(0);
        assertThat(row.getCustomerId()).isNotNull();
        assertThat(row.getCustomerName()).isEqualTo("Acme Co");
    }

    @Test
    @DisplayName("OrderRepository.findProductsForOrders returns all products per order")
    void productsForOrdersBatch() {
        var rows = orderRepository.findAllOrderProducts();

        // Group into map<orderId, list of product ids>
        Map<Long, List<Long>> productsByOrder = rows.stream()
                .collect(Collectors.groupingBy(
                        ProductSummaryView::getOrderId,
                        Collectors.mapping(ProductSummaryView::getProductId, Collectors.toList())
                ));

        assertThat(productsByOrder.get(order1Id))
                .containsExactlyInAnyOrder(p1Id, p2Id);
        assertThat(productsByOrder.get(order2Id))
                .containsExactlyInAnyOrder(p2Id);
    }
}
