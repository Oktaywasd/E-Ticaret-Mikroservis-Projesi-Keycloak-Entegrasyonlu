package com.ecommerce.productcatalog.listener;

import com.ecommerce.productcatalog.dto.event.OrderCancelledEvent;
import com.ecommerce.productcatalog.dto.event.OrderCreatedEvent;
import com.ecommerce.productcatalog.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStockListenerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderStockListener orderStockListener;

    @Test
    @DisplayName("OrderCreatedEvent yakalandığında her sipariş kalemi için reduceStock çağrılmalı")
    void shouldInvokeReduceStock_whenOrderCreatedEventReceived() {
        // Arrange
        OrderCreatedEvent.OrderItemDto item = OrderCreatedEvent.OrderItemDto.builder()
                .productId("prod-1")
                .quantity(2)
                .build();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("order-100")
                .userId("user-abc")
                .items(List.of(item))
                .createdAt(LocalDateTime.now())
                .build();

        // Act
        orderStockListener.handleOrderCreated(event);

        // Assert
        verify(productService, times(1)).reduceStock("prod-1", 2);
    }

    @Test
    @DisplayName("OrderCancelledEvent yakalandığında her sipariş kalemi için restoreStock çağrılmalı")
    void shouldInvokeRestoreStock_whenOrderCancelledEventReceived() {
        // Arrange
        OrderCancelledEvent.CancelledOrderItemDto item = OrderCancelledEvent.CancelledOrderItemDto.builder()
                .productId("prod-1")
                .quantity(2)
                .build();

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId("order-100")
                .keycloakUserId("user-abc")
                .items(List.of(item))
                .build();

        // Act
        orderStockListener.handleOrderCancelled(event);

        // Assert
        verify(productService, times(1)).restoreStock("prod-1", 2);
    }
}