package com.ecommerce.media.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReelCommentResponse {
    private String id;
    private String reelId;
    private String userId;
    private String username;
    private String content;
    private Boolean isVerifiedBuyer;
    private Boolean isPinned;
    private Long likeCount;
    private Instant createdAt;
}