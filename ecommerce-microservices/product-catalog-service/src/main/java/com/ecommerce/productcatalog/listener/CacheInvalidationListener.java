package com.ecommerce.productcatalog.listener;

import com.ecommerce.productcatalog.dto.event.CacheInvalidationEvent;
import com.ecommerce.productcatalog.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationListener {

    private final CacheService cacheService;

    @RabbitListener(queues = "#{autoDeleteCacheQueue.name}")
    public void handleCacheInvalidation(CacheInvalidationEvent event) {
        if (event == null) {
            log.warn("[CACHE FANOUT] Null cache invalidation event received. Skipping.");
            return;
        }

        log.info("[CACHE FANOUT] Invalidation event received: pattern={}, reason={}",
                event.getCachePattern(), event.getTriggeredBy());

        // 1. Event ile gelen özel desen/anahtar varsa temizle
        if (event.getCachePattern() != null && !event.getCachePattern().isBlank()) {
            cacheService.delete(event.getCachePattern());
        }

        // 2. Ürün değişikliklerinden doğrudan etkilenen popüler/top önbellekleri temizle
        cacheService.delete("cache:top_products");
        cacheService.delete("cache:top_50_products");

        log.info("[CACHE FANOUT] Local/Redis caches cleared successfully for reason: {}", event.getTriggeredBy());
    }
}