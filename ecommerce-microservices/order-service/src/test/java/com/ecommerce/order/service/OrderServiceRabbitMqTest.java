package com.ecommerce.order.service;

import com.ecommerce.order.client.CrmClient;
import com.ecommerce.order.client.ProductCatalogClient;
import com.ecommerce.order.client.dto.ProductResponseDto;
import com.ecommerce.order.dto.event.OrderCancelledEvent;
import com.ecommerce.order.dto.event.OrderCreatedEvent;
import com.ecommerce.order.dto.request.CreateOrderRequestDto;
import com.ecommerce.order.dto.request.OrderItemRequestDto;
import com.ecommerce.order.dto.response.OrderResponseDto;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceRabbitMqTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private CrmClient crmClient;

    @Mock
    private OrderMapper orderMapper;

    // Mockito YERİNE Test Spy / Fake RabbitTemplate: Byte Buddy & Java 23 engelini tamamen aşar
    private TestRabbitTemplate fakeRabbitTemplate;

    private OrderServiceImpl orderService;

    // Gönderilen mesajları yakalamak için hafif bir test sınıfı
    static class TestRabbitTemplate extends RabbitTemplate {
        final List<PublishedMessage> publishedMessages = new ArrayList<>();

        record PublishedMessage(String exchange, String routingKey, Object message) {}

        @Override
        public void convertAndSend(String exchange, String routingKey, Object message) throws AmqpException {
            publishedMessages.add(new PublishedMessage(exchange, routingKey, message));
        }
    }

    @BeforeEach
    void setUp() {
        fakeRabbitTemplate = new TestRabbitTemplate();
        orderService = new OrderServiceImpl(
                orderRepository,
                productCatalogClient,
                crmClient,
                orderMapper,
                fakeRabbitTemplate
        );
    }

    @Test
    @DisplayName("createOrder() başarılı olduğunda order.created eventi RabbitMQ'ya fırlatılmalı")
    void shouldPublishOrderCreatedEvent_whenOrderCreatedSuccessfully() {
        // Arrange
        String userId = "keycloak-user-123";
        String addressId = "addr-456";
        String productId = "prod-789";

        OrderItemRequestDto itemDto = new OrderItemRequestDto();
        itemDto.setProductId(productId);
        itemDto.setQuantity(2);

        CreateOrderRequestDto requestDto = new CreateOrderRequestDto();
        requestDto.setAddressId(addressId);
        requestDto.setItems(List.of(itemDto));

        when(crmClient.getAddressById(addressId)).thenReturn(null);

        ProductResponseDto productDto = new ProductResponseDto() {
            @Override
            public String getId() {
                return productId;
            }

            @Override
            public String getName() {
                return "Test Laptop";
            }

            @Override
            public Integer getCurrentStockCount() {
                return 10;
            }

            @Override
            public BigDecimal getEffectivePrice() {
                return BigDecimal.valueOf(1500);
            }
        };

        when(productCatalogClient.getProductById(productId)).thenReturn(productDto);
        when(orderRepository.existsByOrderCode(any())).thenReturn(false);

        Order savedOrder = Order.builder()
                .id("order-id-100")
                .orderCode("ORD-123456")
                .keycloakUserId(userId)
                .addressId(addressId)
                .totalAmount(BigDecimal.valueOf(3000))
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        OrderItem.builder()
                                .productId(productId)
                                .productName("Test Laptop")
                                .unitPrice(BigDecimal.valueOf(1500))
                                .quantity(2)
                                .totalPrice(BigDecimal.valueOf(3000))
                                .build()
                ))
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toOrderResponseDto(any(Order.class))).thenReturn(new OrderResponseDto());

        // Act
        orderService.createOrder(requestDto, userId);

        // Assert: Mesaj listesi kontrolü
        assertThat(fakeRabbitTemplate.publishedMessages).hasSize(1);
        TestRabbitTemplate.PublishedMessage sent = fakeRabbitTemplate.publishedMessages.getFirst();

        assertThat(sent.exchange()).isEqualTo("order.exchange");
        assertThat(sent.routingKey()).isEqualTo("order.created");
        assertThat(sent.message()).isInstanceOf(OrderCreatedEvent.class);

        OrderCreatedEvent event = (OrderCreatedEvent) sent.message();
        assertThat(event.getOrderId()).isEqualTo("order-id-100");
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getItems()).hasSize(1);
        assertThat(event.getItems().getFirst().getProductId()).isEqualTo(productId);
        assertThat(event.getItems().getFirst().getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("cancelOrder() başarılı olduğunda order.cancelled eventi RabbitMQ'ya fırlatılmalı")
    void shouldPublishOrderCancelledEvent_whenOrderCancelledSuccessfully() {
        // Arrange
        String orderId = "order-id-200";
        String userId = "keycloak-user-123";
        String productId = "prod-789";

        Order existingOrder = Order.builder()
                .id(orderId)
                .orderCode("ORD-654321")
                .keycloakUserId(userId)
                .status(OrderStatus.CREATED)
                .items(List.of(
                        OrderItem.builder()
                                .productId(productId)
                                .quantity(3)
                                .build()
                ))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toOrderResponseDto(any(Order.class))).thenReturn(new OrderResponseDto());

        // Act
        orderService.cancelOrder(orderId, userId, false);

        // Assert
        assertThat(existingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        assertThat(fakeRabbitTemplate.publishedMessages).hasSize(1);
        TestRabbitTemplate.PublishedMessage sent = fakeRabbitTemplate.publishedMessages.getFirst();

        assertThat(sent.exchange()).isEqualTo("order.exchange");
        assertThat(sent.routingKey()).isEqualTo("order.cancelled");
        assertThat(sent.message()).isInstanceOf(OrderCancelledEvent.class);

        OrderCancelledEvent event = (OrderCancelledEvent) sent.message();
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getKeycloakUserId()).isEqualTo(userId);
        assertThat(event.getItems()).hasSize(1);
        assertThat(event.getItems().getFirst().getProductId()).isEqualTo(productId);
        assertThat(event.getItems().getFirst().getQuantity()).isEqualTo(3);
    }
}