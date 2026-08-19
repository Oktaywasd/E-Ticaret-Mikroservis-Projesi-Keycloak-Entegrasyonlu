package com.ecommerce.order.service.impl;

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
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductCatalogClient productCatalogClient;
    private final CrmClient crmClient;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponseDto createOrder(CreateOrderRequestDto requestDto, String keycloakUserId) {
        log.info("Creating order for user: {}", keycloakUserId);

        // 1. CRM Service üzerinden adres varlık kontrolü (Adres yoksa Feign ErrorDecoder fırlatır)
        crmClient.getAddressById(requestDto.getAddressId());

        BigDecimal grandTotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // 2. Ürünlerin stok ve güncel fiyat kontrolü
        for (OrderItemRequestDto itemDto : requestDto.getItems()) {
            ProductResponseDto product = productCatalogClient.getProductById(itemDto.getProductId());

            // GÜNCELLENDİ: helper method ile stok kontrolü
            if (product.getCurrentStockCount() < itemDto.getQuantity()) {
                throw new BusinessException("Insufficient stock for product: " + product.getName() +
                        ". Available stock: " + product.getCurrentStockCount());
            }

            // GÜNCELLENDİ: helper method ile geçerli/indirimli fiyat üzerinden birim fiyat tespiti
            BigDecimal unitPrice = product.getEffectivePrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            grandTotal = grandTotal.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(itemDto.getQuantity())
                    .totalPrice(itemTotal)
                    .build();

            orderItems.add(orderItem);
        }

        // 3. Siparişi veritabanına kaydetme
        Order order = Order.builder()
                .keycloakUserId(keycloakUserId)
                .addressId(requestDto.getAddressId())
                .items(orderItems)
                .totalAmount(grandTotal)
                .status(OrderStatus.CREATED)
                .build();

        Order savedOrder = orderRepository.save(order);

        // 4. Product Catalog Service üzerinde stok düşürme çağrıları
        for (OrderItem item : savedOrder.getItems()) {
            productCatalogClient.reduceStock(item.getProductId(), item.getQuantity());
        }

        log.info("Order created successfully with ID: {}", savedOrder.getId());
        return orderMapper.toOrderResponseDto(savedOrder);
    }

    @Override
    public OrderResponseDto getOrderById(String orderId, String keycloakUserId, boolean isAdmin) {
        Order order = findOrderEntityById(orderId);

        if (!isAdmin && !order.getKeycloakUserId().equals(keycloakUserId)) {
            throw new BusinessException("You are not authorized to view this order.");
        }

        return orderMapper.toOrderResponseDto(order);
    }

    @Override
    public List<OrderResponseDto> getMyOrders(String keycloakUserId) {
        List<Order> orders = orderRepository.findByKeycloakUserIdOrderByCreatedAtDesc(keycloakUserId);
        return orderMapper.toOrderResponseDtoList(orders);
    }

    @Override
    public List<OrderResponseDto> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        return orderMapper.toOrderResponseDtoList(orders);
    }

    @Override
    public OrderResponseDto updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = findOrderEntityById(orderId);
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        log.info("Order status updated to {} for order ID: {}", newStatus, orderId);

        return orderMapper.toOrderResponseDto(updatedOrder);
    }

    @Override
    public OrderResponseDto cancelOrder(String orderId, String keycloakUserId, boolean isAdmin) {
        Order order = findOrderEntityById(orderId);

        if (!isAdmin && !order.getKeycloakUserId().equals(keycloakUserId)) {
            throw new BusinessException("You are not authorized to cancel this order.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Delivered orders cannot be cancelled.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order is already cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        log.info("Order cancelled successfully with ID: {}", orderId);

        return orderMapper.toOrderResponseDto(cancelledOrder);
    }
//mavi tik için eklendi
    @Override
    public Boolean verifyUserPurchasedProduct(String userId, String productId) {
        return orderRepository.existsByKeycloakUserIdAndStatusInAndItems_ProductId(
                userId,
                List.of(OrderStatus.DELIVERED, OrderStatus.PAID),
                productId
        );
    }

    private Order findOrderEntityById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED) {
            throw new BusinessException("Cannot change status of an order that is already " + currentStatus);
        }
    }
}