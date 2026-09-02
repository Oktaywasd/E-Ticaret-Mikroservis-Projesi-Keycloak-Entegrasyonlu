package com.ecommerce.productcatalog.listener;

import com.ecommerce.productcatalog.config.RabbitMqConfig;
import com.ecommerce.productcatalog.dto.event.OrderCancelledEvent;
import com.ecommerce.productcatalog.dto.event.OrderCreatedEvent;
import com.ecommerce.productcatalog.service.IdempotencyService;
import com.ecommerce.productcatalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStockListener {

    private final ProductService productService;
    private final IdempotencyService idempotencyService;

    @RabbitListener(queues = RabbitMqConfig.STOCK_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order ID: {}", event.getOrderId());

        // Idempotency: Aynı sipariş için mükerrer stok düşüşünü engelle
        String idempotencyKey = "processed:order:deduct:" + event.getOrderId();
        if (!idempotencyService.processIfFirstTime(idempotencyKey)) {
            log.warn("[IDEMPOTENCY] Order {} already processed for stock deduction. Skipping duplicate event.", event.getOrderId());
            return;
        }

        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("OrderCreatedEvent contains no items for order ID: {}", event.getOrderId());
            return;
        }

        for (OrderCreatedEvent.OrderItemDto item : event.getItems()) {
            log.info("Deducting stock - Product ID: {}, Quantity: {}, Order ID: {}",
                    item.getProductId(), item.getQuantity(), event.getOrderId());

            productService.reduceStock(item.getProductId(), item.getQuantity());
        }

        log.info("All stocks successfully deducted for order ID: {}", event.getOrderId());
    }

    @RabbitListener(queues = RabbitMqConfig.STOCK_RESTORE_QUEUE)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for order ID: {}", event.getOrderId());

        // Idempotency: Aynı iptal için mükerrer stok iadesini engelle
        String idempotencyKey = "processed:order:restore:" + event.getOrderId();
        if (!idempotencyService.processIfFirstTime(idempotencyKey)) {
            log.warn("[IDEMPOTENCY] Order {} already processed for stock restore. Skipping duplicate event.", event.getOrderId());
            return;
        }

        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("OrderCancelledEvent contains no items for order ID: {}", event.getOrderId());
            return;
        }

        for (OrderCancelledEvent.CancelledOrderItemDto item : event.getItems()) {
            log.info("Restoring stock - Product ID: {}, Quantity: {}, Order ID: {}",
                    item.getProductId(), item.getQuantity(), event.getOrderId());
            productService.restoreStock(item.getProductId(), item.getQuantity());
        }

        log.info("All stocks successfully restored for cancelled order ID: {}", event.getOrderId());
    }
}