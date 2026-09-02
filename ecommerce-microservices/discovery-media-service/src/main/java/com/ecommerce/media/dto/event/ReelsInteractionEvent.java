package com.ecommerce.media.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReelsInteractionEvent implements Serializable {

    private String reelId;
    private String userId;
    private InteractionType type;
    private LocalDateTime timestamp;

    public enum InteractionType {
        VIEW,
        LIKE
    }
}