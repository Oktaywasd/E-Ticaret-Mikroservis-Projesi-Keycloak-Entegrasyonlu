package com.ecommerce.order.client;

import com.ecommerce.order.client.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service", url = "${application.config.product-service-url}")
public interface ProductCatalogClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponseDto getProductById(@PathVariable(name = "id") String id);

    @PutMapping("/api/v1/products/{id}/reduce-stock")
    void reduceStock(@PathVariable(name = "id") String id, @RequestParam(name = "quantity") Integer quantity);

    @PutMapping("/api/v1/products/{id}/restore-stock")
    void restoreStock(@PathVariable(name = "id") String id, @RequestParam(name = "quantity") Integer quantity);
}