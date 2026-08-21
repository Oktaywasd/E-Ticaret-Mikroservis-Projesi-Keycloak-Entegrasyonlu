package com.ecommerce.media.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reels")
public class Reel {

    @Id
    private String id;

    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private Integer durationInSeconds;

    private String productId;
    private String sellerId;

    @Builder.Default
    private Long likeCount = 0L;

    @Builder.Default
    private Long viewCount = 0L;

    @Builder.Default
    private Set<String> likedUserIds = new HashSet<>();

    @Builder.Default
    private Set<String> viewedUserIds = new HashSet<>();

    @Builder.Default
    private String status = "ACTIVE";

    @Builder.Default
    private Instant createdAt = Instant.now();
}