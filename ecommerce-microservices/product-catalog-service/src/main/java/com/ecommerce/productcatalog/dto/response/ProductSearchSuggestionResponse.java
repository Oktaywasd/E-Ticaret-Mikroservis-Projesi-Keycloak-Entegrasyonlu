package com.ecommerce.productcatalog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchSuggestionResponse {
    private String id;
    private String name;
    private String brand;
    private BigDecimal price;
    private String imageUrl;
}