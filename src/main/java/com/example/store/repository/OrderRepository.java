package com.example.store.repository;

import com.example.store.entity.Order;

import com.example.store.entity.OrderRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    @org.springframework.data.jpa.repository.QueryHints({
            @jakarta.persistence.QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "1000"),
            @jakarta.persistence.QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_READ_ONLY, value = "true")
    })
    List<OrderRow> findAllRows();
}
