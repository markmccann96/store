package com.example.store.repository;

import com.example.store.entity.Order;

import com.example.store.repository.projection.OrderRow;
import com.example.store.repository.projection.ProductSummaryView;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface OrderRepository extends PagingAndSortingRepository<Order, Long>, JpaRepository<Order, Long> {
    @Query("""
      select o.id as id,
             o.description as description,
             c.id as customerId,
             c.name as customerName
      from Order o join o.customer c
      order by o.createdAt desc, o.id desc
    """)
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "1000"),
            @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_READ_ONLY, value = "true")
    })
    List<OrderRow> findAllRows();

    @Query(
            value = """
        select op.order_id   as orderId,
               op.product_id as productId,
               p.description as productDescription
        from public.order_product op
        join public.product p on p.id = op.product_id
        order by op.order_id asc, op.product_id asc
      """,
            nativeQuery = true
    )
    @QueryHints({
            @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "1000"),
            @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_READ_ONLY, value = "true")
    })
    List<ProductSummaryView> findAllOrderProducts();
}
