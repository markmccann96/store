package com.example.store.mapper;

import com.example.store.api.dto.ProductCreateDTO;
import com.example.store.api.dto.ProductDTO;
import com.example.store.entity.Product;
import com.example.store.entity.Order;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    /** Map create payload → entity (IDs are client-supplied in your schema). */
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "orders", ignore = true)
    Product toEntity(ProductCreateDTO dto);

    /**
     * Map entity → DTO; the orders list is provided explicitly to avoid N+1 / lazy issues.
     * Pass the order IDs you fetched (or an empty list).
     */
    @Mapping(target = "orders", source = "orderIds")
    ProductDTO toDto(Product product, List<Long> orderIds);

    // Overload to map using entity's orders if they are loaded (fallback).
    @AfterMapping
    default void fillOrdersIfMissing(Product src, @MappingTarget ProductDTO dst, List<Long> orderIdsCtx) {
        if (dst.getOrders() == null || dst.getOrders().isEmpty()) {
            if (src.getOrders() != null && !src.getOrders().isEmpty()) {
                List<Long> ids = new ArrayList<>(src.getOrders().size());
                for (Order o : src.getOrders()) {
                    if (o != null && o.getId() != null) {
                        ids.add(o.getId());
                    }
                }
                dst.setOrders(ids);
            } else dst.setOrders(Objects.requireNonNullElseGet(orderIdsCtx, List::of));
        }
    }
}
