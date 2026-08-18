package com.ecommerce.order.model;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    PREPARING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}