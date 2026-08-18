package com.ecommerce.order.controller;

import com.ecommerce.order.dto.request.CreateOrderRequestDto;
import com.ecommerce.order.dto.response.OrderResponseDto;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Controller", description = "Order Management Endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody CreateOrderRequestDto requestDto,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakUserId = jwt.getSubject(); // JWT 'sub' claim'i
        OrderResponseDto response = orderService.createOrder(requestDto, keycloakUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get current authenticated user's orders")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        String keycloakUserId = jwt.getSubject();
        return ResponseEntity.ok(orderService.getMyOrders(keycloakUserId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable("id") String id,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakUserId = jwt.getSubject();
        boolean isAdmin = hasAdminRole(jwt);
        return ResponseEntity.ok(orderService.getOrderById(id, keycloakUserId, isAdmin));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all orders (Admin only)")
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update order status (Admin only)")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable("id") String id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable("id") String id,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakUserId = jwt.getSubject();
        boolean isAdmin = hasAdminRole(jwt);
        return ResponseEntity.ok(orderService.cancelOrder(id, keycloakUserId, isAdmin));
    }

    @SuppressWarnings("unchecked")
    private boolean hasAdminRole(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.contains("ROLE_ADMIN") || roles.contains("ADMIN");
        }
        return false;
    }
}