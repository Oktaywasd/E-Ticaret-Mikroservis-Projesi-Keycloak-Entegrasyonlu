package com.ecommerce.productcatalog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long timeoutInSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, timeoutInSeconds, TimeUnit.SECONDS);
            log.info("Redis SET: key = {} (TTL = {}s)", key, timeoutInSeconds);
        } catch (Exception e) {
            log.error("Redis SET hatası: key = {}, Hata: {}", key, e.getMessage());
        }
    }

    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.info("Cache HIT: key = {}", key);
            } else {
                log.info("Cache MISS: key = {}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("Redis GET hatası: key = {}, Hata: {}", key, e.getMessage());
            return null;
        }
    }

    public void delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("Redis DELETE: key = {} (Success: {})", key, deleted);
        } catch (Exception e) {
            log.error("Redis DELETE hatası: key = {}, Hata: {}", key, e.getMessage());
        }
    }

    public Long getTtl(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis TTL alma hatası: key = {}, Hata: {}", key, e.getMessage());
            return null;
        }
    }
}