package com.ecommerce.productcatalog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    /**
     * İşlemin daha önce yapılıp yapılmadığını atomik olarak kontrol eder.
     * @param key Benzersiz işlem anahtarı (ör: "order:deduct:ord-123")
     * @return true ise işlem İLK DEFA yapılıyordur, güvenle işlenebilir.
     *         false ise işlem DAHA ÖNCE YAPILMIŞTIR (mükerrer), atlanmalıdır.
     */
    public boolean processIfFirstTime(String key) {
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSED", DEFAULT_TTL);
        boolean allowed = Boolean.TRUE.equals(isFirstTime);
        if (!allowed) {
            log.warn("[IDEMPOTENCY] Duplicate event detected for key: {}. Skipping execution.", key);
        }
        return allowed;
    }
}