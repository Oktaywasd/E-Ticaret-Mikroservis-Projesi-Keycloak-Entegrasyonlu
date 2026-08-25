package com.ecommerce.productcatalog.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    private String id;
    private String productId;
    private String userId;
    private String userName;
    private String type; // "REVIEW" veya "QUESTION"
    private Integer rating; // Soru ise null
    private String comment;
    private boolean isVerifiedBuyer;
    private AdminReply adminReply;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminReply {
        private String replyText;
        private String repliedBy;
        private LocalDateTime repliedAt;
    }
}