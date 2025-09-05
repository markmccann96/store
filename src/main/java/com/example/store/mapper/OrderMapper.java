package com.example.store.mapper;


import com.example.store.api.dto.OrderCustomerDTO;
import com.example.store.api.dto.OrderDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
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
            @Mapping(target = "orders", ignore = true)   // important to avoid recursion/unmapped warning
    })
    Customer toCustomer(OrderCustomerDTO src);

}
