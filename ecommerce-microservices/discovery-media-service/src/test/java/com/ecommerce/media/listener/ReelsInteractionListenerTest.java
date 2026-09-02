package com.ecommerce.media.listener;

import com.ecommerce.media.dto.event.ReelsInteractionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReelsInteractionListenerTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ReelsInteractionListener reelsInteractionListener;

    @Test
    @DisplayName("VIEW türünde ReelsInteractionEvent geldiğinde MongoDB'de viewCount 1 artırılmalı")
    void shouldIncrementViewCount_whenViewEventReceived() {
        // Arrange
        String reelId = "reel-abc-123";
        String userId = "user-xyz";

        ReelsInteractionEvent event = ReelsInteractionEvent.builder()
                .reelId(reelId)
                .userId(userId)
                .type(ReelsInteractionEvent.InteractionType.VIEW)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        reelsInteractionListener.handleInteraction(event);

        // Assert: MongoTemplate updateFirst metodunun çağrıldığını doğrula
        verify(mongoTemplate, times(1)).updateFirst(
                any(Query.class),
                any(Update.class),
                eq("reels")
        );
    }

    @Test
    @DisplayName("LIKE türünde ReelsInteractionEvent geldiğinde MongoDB'de likeCount 1 artırılmalı")
    void shouldIncrementLikeCount_whenLikeEventReceived() {
        // Arrange
        String reelId = "reel-abc-123";
        String userId = "user-xyz";

        ReelsInteractionEvent event = ReelsInteractionEvent.builder()
                .reelId(reelId)
                .userId(userId)
                .type(ReelsInteractionEvent.InteractionType.LIKE)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        reelsInteractionListener.handleInteraction(event);

        // Assert
        verify(mongoTemplate, times(1)).updateFirst(
                any(Query.class),
                any(Update.class),
                eq("reels")
        );
    }
}