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
import java.security.SecureRandom;
import java.time.LocalDateTime;
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

        crmClient.getAddressById(requestDto.getAddressId());

        BigDecimal grandTotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequestDto itemDto : requestDto.getItems()) {
            ProductResponseDto product = productCatalogClient.getProductById(itemDto.getProductId());

            if (product == null) {
                throw new ResourceNotFoundException("Product not found with id: " + itemDto.getProductId());
            }

            int currentStock = product.getCurrentStockCount() != null ? product.getCurrentStockCount() : 0;
            if (currentStock < itemDto.getQuantity()) {
                throw new BusinessException("Insufficient stock for product: " + product.getName() +
                        ". Available stock: " + currentStock);
            }

            // Null-safety: EffectivePrice null ise varsayılan fiyat veya 0 al
            BigDecimal unitPrice = product.getEffectivePrice();
            if (unitPrice == null) {
                log.warn("Effective price is null for product: {}. Fallback to ZERO.", product.getId());
                unitPrice = BigDecimal.ZERO;
            }

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

        String uniqueOrderCode = generateUniqueOrderCode();

        Order order = Order.builder()
                .orderCode(uniqueOrderCode)
                .keycloakUserId(keycloakUserId)
                .addressId(requestDto.getAddressId())
                .items(orderItems)
                .totalAmount(grandTotal)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : savedOrder.getItems()) {
            productCatalogClient.reduceStock(item.getProductId(), item.getQuantity());
        }

        log.info("Order created successfully with ID: {} and Code: {}", savedOrder.getId(), uniqueOrderCode);
        return orderMapper.toOrderResponseDto(savedOrder);
    }

    private String generateUniqueOrderCode() {
        SecureRandom random = new SecureRandom();
        String code;
        do {
            int number = 100000 + random.nextInt(900000);
            code = "ORD-" + number;
        } while (orderRepository.existsByOrderCode(code));
        return code;
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
        order.setUpdatedAt(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);
        log.info("Order status updated to {} for order ID: {}", newStatus, orderId);

        return orderMapper.toOrderResponseDto(updatedOrder);
    }

    @Override
    @Transactional
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
        order.setUpdatedAt(LocalDateTime.now());
        Order cancelledOrder = orderRepository.save(order);

        if (cancelledOrder.getItems() != null) {
            for (OrderItem item : cancelledOrder.getItems()) {
                try {
                    productCatalogClient.restoreStock(item.getProductId(), item.getQuantity());
                    log.info("Stock restored for product: {} with quantity: {}", item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to restore stock for product ID: {} in order: {}", item.getProductId(), orderId, e);
                }
            }
        }

        log.info("Order cancelled successfully with ID: {}", orderId);
        return orderMapper.toOrderResponseDto(cancelledOrder);
    }

    @Override
    public Boolean verifyUserPurchasedProduct(String userId, String productId) {
        if (userId == null || productId == null) {
            return false;
        }
        return orderRepository.existsByKeycloakUserIdAndStatusNotAndItems_ProductId(
                userId,
                OrderStatus.CANCELLED,
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