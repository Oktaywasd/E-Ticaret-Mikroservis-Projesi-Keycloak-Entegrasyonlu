package com.ecommerce.order.controller;

import com.ecommerce.order.dto.request.CreateOrderRequestDto;
import com.ecommerce.order.dto.request.OrderItemRequestDto;
import com.ecommerce.order.dto.response.OrderResponseDto;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private CreateOrderRequestDto validCreateRequest;
    private OrderResponseDto orderResponseDto;

    private final String KEYCLOAK_USER_ID = "user_uuid_123";
    private final String ORDER_ID = "ord_001";

    @BeforeEach
    void setUp() {
        OrderItemRequestDto itemDto = new OrderItemRequestDto();
        itemDto.setProductId("prod_101");
        itemDto.setQuantity(2);

        validCreateRequest = new CreateOrderRequestDto();
        validCreateRequest.setAddressId("addr_456");
        validCreateRequest.setItems(List.of(itemDto));

        orderResponseDto = OrderResponseDto.builder()
                .id(ORDER_ID)
                .orderCode("ORD-123456")
                .totalAmount(BigDecimal.valueOf(300.00))
                .status(OrderStatus.CREATED)
                .build();
    }

    @Test
    @DisplayName("Güvenlik: Token'sız istek yapıldığında 401 Unauthorized dönmeli")
    void createOrder_WhenAnonymousUser_ShouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isUnauthorized());

        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    @DisplayName("Başarılı Sipariş Oluşturma: ROLE_CUSTOMER yetkisi ve DTO ile 201 Created dönmeli")
    void createOrder_WhenAuthenticatedUser_ShouldReturn201Created() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequestDto.class), eq(KEYCLOAK_USER_ID)))
                .thenReturn(orderResponseDto);

        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(KEYCLOAK_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ORDER_ID))
                .andExpect(jsonPath("$.orderCode").value("ORD-123456"));

        verify(orderService, times(1)).createOrder(any(CreateOrderRequestDto.class), eq(KEYCLOAK_USER_ID));
    }

    @Test
    @DisplayName("DTO Validasyonu: Boş request gönderildiğinde 400 Bad Request dönmeli")
    void createOrder_WhenInvalidDto_ShouldReturn400BadRequest() throws Exception {
        CreateOrderRequestDto emptyRequest = new CreateOrderRequestDto();

        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(KEYCLOAK_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    @DisplayName("Kendi Siparişlerimi Listeleme: ROLE_CUSTOMER yetkili kullanıcı 200 OK ile listeyi alabilmeli")
    void getMyOrders_WhenAuthenticatedUser_ShouldReturn200Ok() throws Exception {
        when(orderService.getMyOrders(KEYCLOAK_USER_ID)).thenReturn(List.of(orderResponseDto));

        mockMvc.perform(get("/api/v1/orders/my-orders")
                        .with(jwt().jwt(jwt -> jwt.subject(KEYCLOAK_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ORDER_ID));

        verify(orderService, times(1)).getMyOrders(KEYCLOAK_USER_ID);
    }

    @Test
    @DisplayName("RBAC Admin Yetkisiz: Normal müşteri tüm siparişleri listelemeye çalışırsa 403 Forbidden dönmeli")
    void getAllOrders_WhenCustomerRole_ShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(KEYCLOAK_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());

        verify(orderService, never()).getAllOrders();
    }

    @Test
    @DisplayName("RBAC Admin Yetkili: Admin tüm siparişleri 200 OK ile listeleyebilmeli")
    void getAllOrders_WhenAdminRole_ShouldReturn200Ok() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(orderResponseDto));

        mockMvc.perform(get("/api/v1/orders")
                        .with(jwt().jwt(jwt -> jwt.subject(KEYCLOAK_USER_ID))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    @DisplayName("Dahili Servis Çağrısı: Mavi Tik kontrolü (PermitAll) 200 OK dönmeli")
    void verifyPurchase_WhenCalled_ShouldReturnBoolean() throws Exception {
        when(orderService.verifyUserPurchasedProduct(KEYCLOAK_USER_ID, "prod_101")).thenReturn(true);

        mockMvc.perform(get("/api/v1/orders/internal/verify-purchase")
                        .param("userId", KEYCLOAK_USER_ID)
                        .param("productId", "prod_101"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderService, times(1)).verifyUserPurchasedProduct(KEYCLOAK_USER_ID, "prod_101");
    }

    @Test
    @DisplayName("Sipariş İptali: Müşteri kendi siparişini iptal edebilmeli")
    void cancelOrder_WhenAuthenticatedUser_ShouldReturn200Ok() throws Exception {
        orderResponseDto.setStatus(OrderStatus.CANCELLED);
        when(orderService.cancelOrder(eq(ORDER_ID), eq(KEYCLOAK_USER_ID), anyBoolean()))
                .thenReturn(orderResponseDto);

        mockMvc.perform(put("/api/v1/orders/" + ORDER_ID + "/cancel")
                        .with(jwt().jwt(jwt -> {
                            jwt.subject(KEYCLOAK_USER_ID);
                            jwt.claim("realm_access", Map.of("roles", List.of("CUSTOMER")));
                        }).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService, times(1)).cancelOrder(eq(ORDER_ID), eq(KEYCLOAK_USER_ID), eq(false));
    }
}