package com.example.store.mapper;


import com.example.store.api.dto.OrderCustomerDTO;
import com.example.store.api.dto.OrderDTO;
import com.example.store.api.dto.ProductSummaryDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.repository.projection.OrderRow;
import com.example.store.repository.projection.ProductSummaryView;
import org.mapstruct.*;


import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderDTO orderToOrderDTO(Order order);


    Order orderDTOToOrder(OrderDTO orderDTO);

    List<OrderDTO> ordersToOrderDTOs(List<Order> orders);

    @BeanMapping(ignoreByDefault = true)
    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "name", source = "name"),
            @Mapping(target = "orders", ignore = true)
    })
    Customer toCustomer(OrderCustomerDTO src);

    // Creates the OrderDTO from the projection (find-all version)
    @Mapping(target = "customer.id",   source = "customerId")
    @Mapping(target = "customer.name", source = "customerName")
    @Mapping(target = "products",      ignore = true) // map collection of ProductSummaryView -> ProductSummaryDTO
    OrderDTO rowToDto(OrderRow row);

    // Bulk convenience (optional)
    List<OrderDTO> rowsToDtos(List<OrderRow> rows);

    @Mapping(target = "id",          source = "productId")
    @Mapping(target = "description", source = "productDescription")
    ProductSummaryDTO toProductSummaryDto(ProductSummaryView row);

    // If the projection’s products list can be null, ensure we return []
    @AfterMapping
    default void ensureCollections(@MappingTarget OrderDTO dto) {
        if (dto.getProducts() == null) {
            dto.setProducts(new ArrayList<>());
        }
    }

}
