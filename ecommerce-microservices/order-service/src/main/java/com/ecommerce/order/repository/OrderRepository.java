package com.ecommerce.order.repository;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByKeycloakUserIdOrderByCreatedAtDesc(String keycloakUserId);

    List<Order> findAllByOrderByCreatedAtDesc();

    boolean existsByOrderCode(String orderCode);

    Optional<Order> findByOrderCode(String orderCode);
    // Gömülü (embedded) items listesi içindeki productId ve sipariş statü kontrolü
    boolean existsByKeycloakUserIdAndStatusInAndItems_ProductId(
            String keycloakUserId,
            Collection<OrderStatus> statuses,
            String productId
    );
}