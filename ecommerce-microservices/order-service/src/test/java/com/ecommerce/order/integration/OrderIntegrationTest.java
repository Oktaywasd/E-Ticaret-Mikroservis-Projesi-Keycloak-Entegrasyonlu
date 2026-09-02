package com.ecommerce.order.integration;

import com.ecommerce.order.client.CrmClient;
import com.ecommerce.order.client.ProductCatalogClient;
import com.ecommerce.order.dto.request.CreateOrderRequestDto;
import com.ecommerce.order.dto.request.OrderItemRequestDto;
import com.ecommerce.order.dto.response.OrderResponseDto;
import com.ecommerce.order.exception.BusinessException;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.impl.OrderServiceImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = OrderIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
        "application.config.product-service-url=http://localhost:9562",
        "application.config.crm-service-url=http://localhost:9562"
})
class OrderIntegrationTest {

    @Configuration
    @EnableFeignClients(clients = {ProductCatalogClient.class, CrmClient.class})
    @Import({FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
    static class TestConfig {
    }

    private static WireMockServer wireMockServer;

    @Autowired
    private ProductCatalogClient productCatalogClient;

    @Autowired
    private CrmClient crmClient;

    @Mock
    private OrderRepository orderRepository;

    private TestRabbitTemplate fakeRabbitTemplate;

    private OrderServiceImpl orderService;
    private OrderMapper orderMapper;

    private final String USER_ID = "user_abc_123";
    private final String ADDRESS_ID = "addr_xyz_456";
    private final String PRODUCT_ID = "prod_999";

    static class TestRabbitTemplate extends RabbitTemplate {
        final List<PublishedMessage> publishedMessages = new ArrayList<>();
        record PublishedMessage(String exchange, String routingKey, Object message) {}

        @Override
        public void convertAndSend(String exchange, String routingKey, Object message) throws AmqpException {
            publishedMessages.add(new PublishedMessage(exchange, routingKey, message));
        }
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9562));
        wireMockServer.start();
        WireMock.configureFor("localhost", 9562);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        wireMockServer.resetAll();

        fakeRabbitTemplate = new TestRabbitTemplate();
        orderMapper = Mappers.getMapper(OrderMapper.class);
        orderService = new OrderServiceImpl(
                orderRepository,
                productCatalogClient,
                crmClient,
                orderMapper,
                fakeRabbitTemplate
        );
    }

    @Test
    @DisplayName("Sözleşme Başarılı: CRM ve ProductCatalog onay verdiğinde sipariş oluşturulmalı ve asenkron stok eventi iletilmeli")
    void createOrder_WhenExternalServicesSucceed_ShouldCreateOrderAndReduceStock() {
        stubFor(get(urlEqualTo("/api/v1/addresses/" + ADDRESS_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + ADDRESS_ID + "\",\"title\":\"Ev\",\"addressLine\":\"Kadıköy / İstanbul\"}")));

        String productJsonResponse = """
                {
                    "id": "%s",
                    "name": "Koşu Ayakkabısı",
                    "price": {
                        "sellingPrice": 600.00,
                        "discountedPrice": 500.00
                    },
                    "stock": {
                        "currentStock": 10,
                        "minimumStock": 2
                    }
                }
                """.formatted(PRODUCT_ID);

        stubFor(get(urlEqualTo("/api/v1/products/" + PRODUCT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(productJsonResponse)));

        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId("ord_generated_123");
            return o;
        });

        CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                .addressId(ADDRESS_ID)
                .items(List.of(new OrderItemRequestDto(PRODUCT_ID, 2)))
                .build();

        OrderResponseDto response = orderService.createOrder(request, USER_ID);

        assertNotNull(response);
        assertEquals("ord_generated_123", response.getId());
        assertEquals(0, new BigDecimal("1000.00").compareTo(response.getTotalAmount()));
        assertEquals(OrderStatus.CREATED, response.getStatus());

        assertFalse(fakeRabbitTemplate.publishedMessages.isEmpty());
        assertEquals("order.exchange", fakeRabbitTemplate.publishedMessages.getFirst().exchange());
        assertEquals("order.created", fakeRabbitTemplate.publishedMessages.getFirst().routingKey());
    }

    @Test
    @DisplayName("Sözleşme Stok Hatası: Yetersiz stok durumunda BusinessException fırlatılmalı")
    void createOrder_WhenInsufficientStock_ShouldThrowBusinessException() {
        stubFor(get(urlEqualTo("/api/v1/addresses/" + ADDRESS_ID))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + ADDRESS_ID + "\"}")));

        String productJsonResponse = """
                {
                    "id": "%s",
                    "name": "Koşu Ayakkabısı",
                    "price": {
                        "sellingPrice": 500.00,
                        "discountedPrice": null
                    },
                    "stock": {
                        "currentStock": 1,
                        "minimumStock": 0
                    }
                }
                """.formatted(PRODUCT_ID);

        stubFor(get(urlEqualTo("/api/v1/products/" + PRODUCT_ID))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(productJsonResponse)));

        CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                .addressId(ADDRESS_ID)
                .items(List.of(new OrderItemRequestDto(PRODUCT_ID, 3)))
                .build();

        BusinessException exception = assertThrows(BusinessException.class, () ->
                orderService.createOrder(request, USER_ID)
        );

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(orderRepository, never()).save(any());
        assertTrue(fakeRabbitTemplate.publishedMessages.isEmpty());
    }

    @Test
    @DisplayName("Sözleşme İptal Akışı: Sipariş iptal edildiğinde restoreStock eventi iletilmeli")
    void cancelOrder_WhenCancelled_ShouldRestoreStockViaEvent() {
        OrderItem item = OrderItem.builder()
                .productId(PRODUCT_ID)
                .quantity(2)
                .build();

        Order existingOrder = Order.builder()
                .id("ord_cancel_101")
                .keycloakUserId(USER_ID)
                .status(OrderStatus.CREATED)
                .items(List.of(item))
                .build();

        when(orderRepository.findById("ord_cancel_101")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDto cancelled = orderService.cancelOrder("ord_cancel_101", USER_ID, false);

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertFalse(fakeRabbitTemplate.publishedMessages.isEmpty());
        assertEquals("order.exchange", fakeRabbitTemplate.publishedMessages.getFirst().exchange());
        assertEquals("order.cancelled", fakeRabbitTemplate.publishedMessages.getFirst().routingKey());
    }
}