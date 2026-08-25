package com.ecommerce.productcatalog.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${order-service.url:http://localhost:8083}")
public interface OrderClient {

    @GetMapping("/api/v1/orders/internal/verify-purchase")
    Boolean verifyPurchase(@RequestParam("userId") String userId,
                           @RequestParam("productId") String productId);
}