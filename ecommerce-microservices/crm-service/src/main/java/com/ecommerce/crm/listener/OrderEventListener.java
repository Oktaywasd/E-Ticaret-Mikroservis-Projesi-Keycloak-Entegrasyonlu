package com.ecommerce.crm.listener;

import com.ecommerce.crm.config.RabbitMqConfig;
import com.ecommerce.crm.dto.event.OrderCancelledEvent;
import com.ecommerce.crm.dto.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    @RabbitListener(queues = RabbitMqConfig.CRM_ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[CRM NOTIFICATION] New order received! Order ID: {}, User ID: {}, Item Count: {}",
                event.getOrderId(), event.getUserId(), event.getItems() != null ? event.getItems().size() : 0);
        // İsteğe bağlı: Burada e-posta/SMS gönderimi veya veritabanına bildirim kaydı atılabilir.
    }

    @RabbitListener(queues = RabbitMqConfig.CRM_ORDER_CANCELLED_QUEUE)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.warn("[CRM NOTIFICATION] Order has been cancelled! Order ID: {}, User ID: {}",
                event.getOrderId(), event.getKeycloakUserId());
        // İsteğe bağlı: Kullanıcıya iptal bilgilendirmesi gönderme işlemi tetiklenebilir.
    }
}