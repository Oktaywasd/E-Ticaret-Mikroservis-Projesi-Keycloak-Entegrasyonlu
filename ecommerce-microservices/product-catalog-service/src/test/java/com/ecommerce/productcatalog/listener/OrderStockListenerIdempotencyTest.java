package com.ecommerce.productcatalog.listener;

import com.ecommerce.productcatalog.dto.event.OrderCreatedEvent;
import com.ecommerce.productcatalog.service.IdempotencyService;
import com.ecommerce.productcatalog.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStockListenerIdempotencyTest {

    @Mock
    private ProductService productService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private OrderStockListener orderStockListener;

    @Test
    @DisplayName("Aynı event iki kez geldiğinde stok düşürme işlemi yalnızca 1 kez çalışmalıdır")
    void shouldProcessOrderStockOnlyOnceWhenDuplicateEventReceived() {
        // Given
        String orderId = "ord-9999";
        OrderCreatedEvent.OrderItemDto item = new OrderCreatedEvent.OrderItemDto("prod-1", 2);
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .items(List.of(item))
                .build();

        // 1. çağrıda true (ilk kez), 2. çağrıda false (mükerrer) dönsün
        when(idempotencyService.processIfFirstTime("processed:order:deduct:" + orderId))
                .thenReturn(true)
                .thenReturn(false);

        // When - Mesaj ilk kez geliyor
        orderStockListener.handleOrderCreated(event);

        // When - Aynı mesaj ağ hatası veya retry sebebiyle tekrar geliyor
        orderStockListener.handleOrderCreated(event);

        // Then
        verify(idempotencyService, times(2)).processIfFirstTime("processed:order:deduct:" + orderId);
        // reduceStock SADECE 1 KEZ tetiklenmeli!
        verify(productService, times(1)).reduceStock("prod-1", 2);
    }
}