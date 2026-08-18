package com.ecommerce.media.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReelResponse {
    private String id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private Integer durationInSeconds;
    private String productId;
    private String sellerId;
    private Long likeCount;
    private Long viewCount;
    private String status;
    private Instant createdAt;

    // Feign ile Catalog servisinden doldurulacak ürün özeti
    private ProductSummaryResponse product;
}