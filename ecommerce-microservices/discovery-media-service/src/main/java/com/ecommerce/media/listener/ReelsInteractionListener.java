package com.ecommerce.media.listener;

import com.ecommerce.media.config.ReelsRabbitMqConfig;
import com.ecommerce.media.dto.event.ReelsInteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReelsInteractionListener {

    private final MongoTemplate mongoTemplate;

    @RabbitListener(queues = ReelsRabbitMqConfig.REELS_INTERACTION_QUEUE)
    public void handleInteraction(ReelsInteractionEvent event) {
        log.info("Processing reel interaction: type={}, reelId={}, userId={}",
                event.getType(), event.getReelId(), event.getUserId());

        Query query = new Query(Criteria.where("id").is(event.getReelId()));
        Update update = new Update();

        if (event.getType() == ReelsInteractionEvent.InteractionType.VIEW) {
            update.inc("viewCount", 1);
        } else if (event.getType() == ReelsInteractionEvent.InteractionType.LIKE) {
            update.inc("likeCount", 1);
        }

        // MongoDB üzerindeki reel dokümanını günceller
        mongoTemplate.updateFirst(query, update, "reels");
        log.info("Reel counter updated in MongoDB for ID: {}", event.getReelId());
    }
}