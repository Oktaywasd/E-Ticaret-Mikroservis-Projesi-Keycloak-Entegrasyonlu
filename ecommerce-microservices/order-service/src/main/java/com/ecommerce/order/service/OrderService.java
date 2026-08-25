package com.ecommerce.order.service;

import com.ecommerce.order.dto.request.CreateOrderRequestDto;
import com.ecommerce.order.dto.response.OrderResponseDto;
import com.ecommerce.order.model.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponseDto createOrder(CreateOrderRequestDto requestDto, String keycloakUserId);

    OrderResponseDto getOrderById(String orderId, String keycloakUserId, boolean isAdmin);

    List<OrderResponseDto> getMyOrders(String keycloakUserId);

    List<OrderResponseDto> getAllOrders();

    OrderResponseDto updateOrderStatus(String orderId, OrderStatus newStatus);

    OrderResponseDto cancelOrder(String orderId, String keycloakUserId, boolean isAdmin);

    Boolean verifyUserPurchasedProduct(String userId, String productId);
}