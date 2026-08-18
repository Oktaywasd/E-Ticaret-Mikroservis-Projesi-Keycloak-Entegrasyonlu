package com.ecommerce.order.mapper;

import com.ecommerce.order.dto.response.OrderItemResponseDto;
import com.ecommerce.order.dto.response.OrderResponseDto;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponseDto toOrderResponseDto(Order order);

    OrderItemResponseDto toOrderItemResponseDto(OrderItem orderItem);

    List<OrderResponseDto> toOrderResponseDtoList(List<Order> orders);
}