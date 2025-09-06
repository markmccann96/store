package com.example.store.repository;


import com.example.store.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends
        PagingAndSortingRepository<Product, Long>,
        JpaRepository<Product, Long> {

    /** Load a single product with its orders (avoids N+1). */
    @EntityGraph(attributePaths = "orders")
    Optional<Product> findWithOrdersById(Long id);

    /** Page through products with orders pre-fetched. */
    @EntityGraph(attributePaths = "orders")
    Page<Product> findAll(Pageable pageable);

    /** Fetch all products with orders pre-fetched. */
    @EntityGraph(attributePaths = "orders")
    List<Product> findAll();

    /** IDs of orders that contain the given product. */
    @Query("select o.id from Order o join o.products p where p.id = :productId")
    List<Long> findOrderIdsByProductId(@Param("productId") Long productId);

    /** All products contained in a specific order (useful for /orders/{id}). */
    @Query("select p from Order o join o.products p where o.id = :orderId")
    List<Product> findByOrderId(@Param("orderId") Long orderId);
}
