package com.ecommerce.productcatalog.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private String id;
    private String productId;
    private String userId;
    private String userName;
    private String type;
    private Integer rating;
    private String comment;
    private boolean isVerifiedBuyer;
    private AdminReplyDto adminReply;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class AdminReplyDto {
        private String replyText;
        private String repliedBy;
        private LocalDateTime repliedAt;
    }
}