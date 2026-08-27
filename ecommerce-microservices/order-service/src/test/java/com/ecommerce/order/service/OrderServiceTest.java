package com.ecommerce.order.service;

import com.ecommerce.order.client.CrmClient;
import com.ecommerce.order.client.ProductCatalogClient;
import com.ecommerce.order.client.dto.ProductResponseDto;
import com.ecommerce.order.dto.request.CreateOrderRequestDto;
import com.ecommerce.order.dto.request.OrderItemRequestDto;
import com.ecommerce.order.dto.response.OrderResponseDto;
import com.ecommerce.order.exception.BusinessException;
import com.ecommerce.order.exception.ResourceNotFoundException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private CrmClient crmClient;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CreateOrderRequestDto createOrderRequestDto;
    private ProductResponseDto productResponseDto;
    private Order order;
    private OrderResponseDto orderResponseDto;

    private final String KEYCLOAK_USER_ID = "user_uuid_123";
    private final String OTHER_USER_ID = "user_uuid_999";
    private final String ORDER_ID = "ord_001";
    private final String PRODUCT_ID = "prod_101";

    @BeforeEach
    void setUp() {
        OrderItemRequestDto itemRequestDto = new OrderItemRequestDto();
        itemRequestDto.setProductId(PRODUCT_ID);
        itemRequestDto.setQuantity(2);

        createOrderRequestDto = new CreateOrderRequestDto();
        createOrderRequestDto.setAddressId("addr_456");
        createOrderRequestDto.setItems(List.of(itemRequestDto));

        // ProductResponseDto Mock Nesnesi Hazırlama
        ProductResponseDto.PriceDto priceDto = ProductResponseDto.PriceDto.builder()
                .sellingPrice(BigDecimal.valueOf(150.00))
                .build();

        ProductResponseDto.StockDto stockDto = ProductResponseDto.StockDto.builder()
                .currentStock(10)
                .build();

        productResponseDto = ProductResponseDto.builder()
                .id(PRODUCT_ID)
                .name("Oversize Tişört")
                .price(priceDto)
                .stock(stockDto)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(PRODUCT_ID)
                .productName("Oversize Tişört")
                .unitPrice(BigDecimal.valueOf(150.00))
                .quantity(2)
                .totalPrice(BigDecimal.valueOf(300.00))
                .build();

        order = Order.builder()
                .id(ORDER_ID)
                .orderCode("ORD-123456")
                .keycloakUserId(KEYCLOAK_USER_ID)
                .addressId("addr_456")
                .items(List.of(orderItem))
                .totalAmount(BigDecimal.valueOf(300.00))
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        orderResponseDto = OrderResponseDto.builder()
                .id(ORDER_ID)
                .orderCode("ORD-123456")
                .totalAmount(BigDecimal.valueOf(300.00))
                .status(OrderStatus.CREATED)
                .build();
    }

    @Test
    @DisplayName("Başarılı Sipariş Oluşturma: Sepet tutarı hesaplanmalı ve stok düşürme Feign çağrısı yapılmalı")
    void createOrder_WhenValidRequest_ShouldCalculateTotalAndReduceStock() {
        // Arrange
        when(crmClient.getAddressById("addr_456")).thenReturn(null);
        when(productCatalogClient.getProductById(PRODUCT_ID)).thenReturn(productResponseDto);
        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toOrderResponseDto(order)).thenReturn(orderResponseDto);

        // Act
        OrderResponseDto result = orderService.createOrder(createOrderRequestDto, KEYCLOAK_USER_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderCode()).isEqualTo("ORD-123456");
        verify(productCatalogClient, times(1)).reduceStock(PRODUCT_ID, 2);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Yetersiz Stok: İstenen miktar mevcut stoktan fazlaysa BusinessException fırlatmalı")
    void createOrder_WhenInsufficientStock_ShouldThrowBusinessException() {
        // Arrange (Mevcut stok: 1, İstenen: 2)
        productResponseDto.getStock().setCurrentStock(1);
        when(crmClient.getAddressById("addr_456")).thenReturn(null);
        when(productCatalogClient.getProductById(PRODUCT_ID)).thenReturn(productResponseDto);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.createOrder(createOrderRequestDto, KEYCLOAK_USER_ID);
        });

        assertThat(exception.getMessage()).contains("Insufficient stock");
        verify(orderRepository, never()).save(any(Order.class));
        verify(productCatalogClient, never()).reduceStock(anyString(), anyInt());
    }

    @Test
    @DisplayName("Sipariş Detay Görme (Kullanıcı İzolasyonu): Başkasının siparişine bakan kullanıcı BusinessException almalı")
    void getOrderById_WhenUserNotOwnerAndNotAdmin_ShouldThrowBusinessException() {
        // Arrange
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.getOrderById(ORDER_ID, OTHER_USER_ID, false);
        });
    }

    @Test
    @DisplayName("Sipariş Detay Görme (Admin Yetkisi): Admin başkasının siparişini görüntüleyebilmeli")
    void getOrderById_WhenAdminViewsOtherUserOrder_ShouldReturnOrder() {
        // Arrange
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderMapper.toOrderResponseDto(order)).thenReturn(orderResponseDto);

        // Act
        OrderResponseDto result = orderService.getOrderById(ORDER_ID, OTHER_USER_ID, true);

        // Assert
        assertThat(result).isNotNull();
        verify(orderMapper, times(1)).toOrderResponseDto(order);
    }

    @Test
    @DisplayName("Başarılı Sipariş İptali: İptal edilen sipariş için restoreStock tetiklenmeli")
    void cancelOrder_WhenValid_ShouldSetStatusCancelledAndRestoreStock() {
        // Arrange
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toOrderResponseDto(order)).thenReturn(orderResponseDto);

        // Act
        orderService.cancelOrder(ORDER_ID, KEYCLOAK_USER_ID, false);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(productCatalogClient, times(1)).restoreStock(PRODUCT_ID, 2);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("Teslim Edilmiş Sipariş İptal Edilemez: DELIVERED statüsündeki sipariş iptal edilmek istenirse BusinessException fırlatmalı")
    void cancelOrder_WhenDelivered_ShouldThrowBusinessException() {
        // Arrange
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            orderService.cancelOrder(ORDER_ID, KEYCLOAK_USER_ID, false);
        });

        assertThat(exception.getMessage()).contains("Delivered orders cannot be cancelled");
        verify(productCatalogClient, never()).restoreStock(anyString(), anyInt());
    }

    @Test
    @DisplayName("Geçersiz Durum Geçişi: DELIVERED veya CANCELLED durumundaki siparişin statüsü değiştirilemez")
    void updateOrderStatus_WhenOrderAlreadyDelivered_ShouldThrowBusinessException() {
        // Arrange
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            orderService.updateOrderStatus(ORDER_ID, OrderStatus.SHIPPED);
        });
    }

    @Test
    @DisplayName("Reels Mavi Tik Doğrulaması: Kullanıcı ürünü satın almışsa true dönmeli")
    void verifyUserPurchasedProduct_WhenPurchased_ShouldReturnTrue() {
        // Arrange
        when(orderRepository.existsByKeycloakUserIdAndStatusNotAndItems_ProductId(
                KEYCLOAK_USER_ID, OrderStatus.CANCELLED, PRODUCT_ID
        )).thenReturn(true);

        // Act
        Boolean isVerified = orderService.verifyUserPurchasedProduct(KEYCLOAK_USER_ID, PRODUCT_ID);

        // Assert
        assertThat(isVerified).isTrue();
    }
}