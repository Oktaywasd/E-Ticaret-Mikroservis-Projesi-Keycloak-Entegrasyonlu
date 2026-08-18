package com.ecommerce.media.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reel_comments")
public class ReelComment {

    @Id
    private String id;

    @Indexed
    private String reelId;

    private String userId;
    private String username;
    private String content;

    @Builder.Default
    private Boolean isVerifiedBuyer = false;

    @Builder.Default
    private Boolean isPinned = false;

    @Builder.Default
    private Long likeCount = 0L;

    @Builder.Default
    private Instant createdAt = Instant.now();
}