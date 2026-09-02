package com.ecommerce.crm.listener;

import com.ecommerce.crm.dto.event.OrderCancelledEvent;
import com.ecommerce.crm.dto.event.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OrderEventListenerTest {

    private OrderEventListener orderEventListener;

    @BeforeEach
    void setUp() {
        orderEventListener = new OrderEventListener();
    }

    @Test
    @DisplayName("handleOrderCreated() metodu geçerli bir OrderCreatedEvent aldığında hatasız tamamlanmalı")
    void shouldHandleOrderCreatedEventSuccessfully() {
        // Arrange
        OrderCreatedEvent.OrderItemDto item = OrderCreatedEvent.OrderItemDto.builder()
                .productId("prod-101")
                .quantity(2)
                .build();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("ord-crm-555")
                .userId("keycloak-user-crm-1")
                .items(List.of(item))
                .createdAt(LocalDateTime.now())
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> orderEventListener.handleOrderCreated(event));
    }

    @Test
    @DisplayName("handleOrderCreated() metodu items listesi null olduğunda da NullPointerException fırlatmadan çalışmalı")
    void shouldHandleOrderCreatedEvent_whenItemsNull() {
        // Arrange
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("ord-crm-556")
                .userId("keycloak-user-crm-2")
                .items(null)
                .createdAt(LocalDateTime.now())
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> orderEventListener.handleOrderCreated(event));
    }

    @Test
    @DisplayName("handleOrderCancelled() metodu geçerli bir OrderCancelledEvent aldığında hatasız tamamlanmalı")
    void shouldHandleOrderCancelledEventSuccessfully() {
        // Arrange
        OrderCancelledEvent.CancelledOrderItemDto item = OrderCancelledEvent.CancelledOrderItemDto.builder()
                .productId("prod-101")
                .quantity(2)
                .build();

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId("ord-crm-555")
                .keycloakUserId("keycloak-user-crm-1")
                .items(List.of(item))
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> orderEventListener.handleOrderCancelled(event));
    }
}