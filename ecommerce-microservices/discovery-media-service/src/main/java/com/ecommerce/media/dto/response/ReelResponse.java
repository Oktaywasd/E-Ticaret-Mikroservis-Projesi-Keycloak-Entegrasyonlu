package com.ecommerce.media.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private Set<String> likedUserIds;
    private Set<String> viewedUserIds;
    private String status;
    private Instant createdAt;

    private String productName;
    private ProductSummaryResponse product;

    private Long commentCount;
}