package com.ecommerce.media.client;

import com.ecommerce.media.dto.response.ProductSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-catalog-service", url = "${application.clients.product-catalog.url:http://localhost:8081}")
public interface ProductCatalogClient {

    @GetMapping("/api/v1/products/{id}")
    ProductSummaryResponse getProductSummaryById(@PathVariable("id") String id);
}