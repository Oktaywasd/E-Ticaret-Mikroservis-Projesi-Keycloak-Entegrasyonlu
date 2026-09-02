package com.ecommerce.order.e2e;

import com.ecommerce.order.client.CrmClient;
import com.ecommerce.order.client.ProductCatalogClient;
import com.ecommerce.order.dto.request.CreateOrderRequestDto;
import com.ecommerce.order.dto.request.OrderItemRequestDto;
import com.ecommerce.order.dto.response.OrderResponseDto;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.Order;
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

@SpringBootTest(classes = OrderWorkflowE2ETest.TestConfig.class)
@TestPropertySource(properties = {
        "application.config.product-service-url=http://localhost:9570",
        "application.config.crm-service-url=http://localhost:9570"
})
class OrderWorkflowE2ETest {

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

    // Mockito YERİNE Test Spy / Fake Template (Java 23 uyumlu)
    private TestRabbitTemplate fakeRabbitTemplate;

    private OrderServiceImpl orderService;
    private OrderMapper orderMapper;

    private final String CUSTOMER_ID = "cust_e2e_777";
    private final String ADDRESS_ID = "addr_e2e_888";
    private final String PRODUCT_ID = "prod_e2e_999";

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
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9570));
        wireMockServer.start();
        WireMock.configureFor("localhost", 9570);
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
    @DisplayName("Senaryo 1 (Happy Path): Adres ve ürün doğrulanır, sipariş verilir ve asenkron stok event fırlatılır")
    void fullOrderLifecycle_ShouldCreateOrderAndReduceStock() {
        stubFor(get(urlEqualTo("/api/v1/addresses/" + ADDRESS_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + ADDRESS_ID + "\",\"title\":\"Ev Adresi\"}")));

        String productJson = """
                {
                    "id": "%s",
                    "name": "Kablosuz Kulaklık",
                    "price": {
                        "sellingPrice": 1500.00,
                        "discountedPrice": 1200.00
                    },
                    "stock": {
                        "currentStock": 20,
                        "minimumStock": 2
                    }
                }
                """.formatted(PRODUCT_ID);

        stubFor(get(urlEqualTo("/api/v1/products/" + PRODUCT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(productJson)));

        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId("ord_e2e_001");
            return o;
        });

        CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                .addressId(ADDRESS_ID)
                .items(List.of(new OrderItemRequestDto(PRODUCT_ID, 2)))
                .build();

        OrderResponseDto response = orderService.createOrder(request, CUSTOMER_ID);

        assertNotNull(response);
        assertEquals("ord_e2e_001", response.getId());
        assertEquals(0, new BigDecimal("2400.00").compareTo(response.getTotalAmount()));
        assertEquals(OrderStatus.CREATED, response.getStatus());

        // RabbitMQ mesajının exchange'e gittiğini doğrula
        assertFalse(fakeRabbitTemplate.publishedMessages.isEmpty());
        assertEquals("order.exchange", fakeRabbitTemplate.publishedMessages.getFirst().exchange());
        assertEquals("order.created", fakeRabbitTemplate.publishedMessages.getFirst().routingKey());
    }

    @Test
    @DisplayName("Senaryo 3 (Telafi / Rollback): İptal edilen sipariş için stok iade eventi fırlatılır")
    void cancelOrderWorkflow_ShouldRestoreStockSuccessfully() {
        Order mockOrder = Order.builder()
                .id("ord_cancel_999")
                .keycloakUserId(CUSTOMER_ID)
                .status(OrderStatus.CREATED)
                .items(List.of(com.ecommerce.order.model.OrderItem.builder()
                        .productId(PRODUCT_ID)
                        .quantity(3)
                        .build()))
                .build();

        when(orderRepository.findById("ord_cancel_999")).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDto cancelled = orderService.cancelOrder("ord_cancel_999", CUSTOMER_ID, false);

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertFalse(fakeRabbitTemplate.publishedMessages.isEmpty());
        assertEquals("order.exchange", fakeRabbitTemplate.publishedMessages.getFirst().exchange());
        assertEquals("order.cancelled", fakeRabbitTemplate.publishedMessages.getFirst().routingKey());
    }

    @Test
    @DisplayName("Senaryo 1 & 4 Doğrulama: İptal edilmemiş siparişi olan müşteri için verifyUserPurchasedProduct true döner")
    void verifyUserPurchasedProduct_ShouldReturnTrueForValidPurchaser() {
        when(orderRepository.existsByKeycloakUserIdAndStatusNotAndItems_ProductId(
                CUSTOMER_ID, OrderStatus.CANCELLED, PRODUCT_ID
        )).thenReturn(true);

        Boolean hasPurchased = orderService.verifyUserPurchasedProduct(CUSTOMER_ID, PRODUCT_ID);

        assertTrue(hasPurchased);
    }
}