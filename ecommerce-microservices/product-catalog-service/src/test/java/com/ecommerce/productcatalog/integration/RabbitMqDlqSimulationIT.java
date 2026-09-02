package com.ecommerce.productcatalog.integration;

import com.ecommerce.productcatalog.config.RabbitMqConfig;
import com.ecommerce.productcatalog.dto.event.OrderCreatedEvent;
import com.ecommerce.productcatalog.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672",
        "spring.rabbitmq.username=guest",
        "spring.rabbitmq.password=guest",
        "spring.rabbitmq.listener.simple.default-requeue-rejected=false",
        "spring.rabbitmq.listener.simple.retry.enabled=true",
        "spring.rabbitmq.listener.simple.retry.max-attempts=3",
        "spring.rabbitmq.listener.simple.retry.initial-interval=100ms"
})
class RabbitMqDlqSimulationIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("Hata alan sipariş mesajı retry limitini aştığında DLQ kuyruğuna iletilmeli")
    void shouldRouteToDlq_whenConsumerFailsRepeatedly() {
        // Arrange: Service çağrıldığında kasıtlı hata fırlat
        doThrow(new RuntimeException("Simulated Database Error"))
                .when(productService).reduceStock(anyString(), anyInt());

        OrderCreatedEvent.OrderItemDto item = OrderCreatedEvent.OrderItemDto.builder()
                .productId("failed-prod-100")
                .quantity(1)
                .build();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("simulated-order-dlq-1")
                .userId("test-user-dlq")
                .items(List.of(item))
                .createdAt(LocalDateTime.now())
                .build();

        // Act: Sipariş oluşturma mesajını ana exchange'e gönder
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDER_EXCHANGE,
                RabbitMqConfig.STOCK_ROUTING_KEY,
                event
        );

        // Assert: Awaitility ile mesajın DLQ kuyruğuna (product.stock.deduct.dlq) düşmesini bekle
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    Message dlqMessage = rabbitTemplate.receive(RabbitMqConfig.STOCK_DLQ);
                    assertThat(dlqMessage).isNotNull();

                    String body = new String(dlqMessage.getBody());
                    assertThat(body).contains("simulated-order-dlq-1");
                    assertThat(body).contains("failed-prod-100");
                });
    }
}