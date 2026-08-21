package com.ecommerce.media.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductSummaryResponse {
    private String id;
    private String name;
    private String productCode;
    private Object price;
    private String thumbnailUrl;
}