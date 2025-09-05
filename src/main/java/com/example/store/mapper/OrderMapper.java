package com.example.store.mapper;


import com.example.store.api.dto.OrderCustomerDTO;
import com.example.store.api.dto.OrderDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.OrderRow;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


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

    // creates the order dto from projection for the find all version.
    @Mapping(target = "customer.id", source = "customerId")
    @Mapping(target = "customer.name", source = "customerName")
    OrderDTO rowToDto(OrderRow row);

    List<OrderDTO> rowsToDtos(List<OrderRow> rows);

}
