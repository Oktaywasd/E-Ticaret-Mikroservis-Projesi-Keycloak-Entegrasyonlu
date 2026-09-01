package com.ecommerce.order.integration;

import com.ecommerce.order.dto.request.CreateOrderRequestDto;
import com.ecommerce.order.dto.request.OrderItemRequestDto;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.isOneOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OrderIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderRepository orderRepository;

    private final String USER_ID = "user_full_flow_123";
    private final String PRODUCT_ID = "prod_full_999";
    private final String ADDRESS_ID = "addr_full_456";

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(9565));
        wireMockServer.start();
        WireMock.configureFor("localhost", 9565);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("application.config.product-service-url", () -> "http://localhost:9565");
        registry.add("application.config.crm-service-url", () -> "http://localhost:9565");
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }

    @Test
    @DisplayName("Uçtan Uca Sipariş Akışı: MockMvc -> Controller -> Service -> WireMock Feign -> 200/201")
    void createOrder_FullFlow_ShouldSaveAndReturnResponse() throws Exception {
        // 1. WireMock: CRM Adres Getirme
        stubFor(get(urlEqualTo("/api/v1/addresses/" + ADDRESS_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + ADDRESS_ID + "\",\"title\":\"Ev\",\"addressLine\":\"Beşiktaş / İstanbul\"}")));

        // 2. WireMock: ProductCatalog Ürün Sorgusu (Fiyat: 250.00, Stok: 20)
        String productJsonResponse = """
                {
                    "id": "%s",
                    "name": "Oversize Hoodie",
                    "price": {
                        "sellingPrice": 300.00,
                        "discountedPrice": 250.00
                    },
                    "stock": {
                        "currentStock": 20,
                        "minimumStock": 5
                    }
                }
                """.formatted(PRODUCT_ID);

        stubFor(get(urlEqualTo("/api/v1/products/" + PRODUCT_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(productJsonResponse)));

        // 3. WireMock: ProductCatalog Stok Düşürme
        stubFor(put(urlMatching("/api/v1/products/" + PRODUCT_ID + "/reduce-stock.*"))
                .willReturn(aResponse().withStatus(200)));

        // 4. DB Mock Davranışı
        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId("ord_generated_123");
            return o;
        });

        // 5. İstek Gövdesi
        OrderItemRequestDto itemDto = new OrderItemRequestDto();
        itemDto.setProductId(PRODUCT_ID);
        itemDto.setQuantity(2);

        CreateOrderRequestDto requestDto = CreateOrderRequestDto.builder()
                .addressId(ADDRESS_ID)
                .items(List.of(itemDto))
                .build();

        // 6. MockMvc: Keycloak Rol ve Authority'leri İçeren JWT ile İstek
        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                                        new SimpleGrantedAuthority("USER"),
                                        new SimpleGrantedAuthority("CUSTOMER")
                                )
                                .jwt(builder -> builder
                                        .subject(USER_ID)
                                        .claim("preferred_username", "testuser")
                                        .claim("email", "test@ecommerce.com")
                                        .claim("realm_access", Map.of("roles", List.of("USER", "CUSTOMER", "user", "customer")))
                                        .claim("resource_access", Map.of("order-service", Map.of("roles", List.of("USER", "CUSTOMER"))))
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().is(isOneOf(200, 201)))
                .andExpect(jsonPath("$.id").value("ord_generated_123"))
                .andExpect(jsonPath("$.orderCode").exists())
                .andExpect(jsonPath("$.totalAmount").value(500.00))
                .andExpect(jsonPath("$.status").value("CREATED"));

        // 7. Feign HTTP Doğrulaması
        verify(putRequestedFor(urlMatching("/api/v1/products/" + PRODUCT_ID + "/reduce-stock.*")));
    }

    @Test
    @DisplayName("Güvenlik Doğrulaması: JWT olmadan atılan istek 401 Unauthorized dönmeli")
    void createOrder_WithoutToken_ShouldReturn401() throws Exception {
        CreateOrderRequestDto requestDto = CreateOrderRequestDto.builder()
                .addressId(ADDRESS_ID)
                .items(List.of())
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
    }
}