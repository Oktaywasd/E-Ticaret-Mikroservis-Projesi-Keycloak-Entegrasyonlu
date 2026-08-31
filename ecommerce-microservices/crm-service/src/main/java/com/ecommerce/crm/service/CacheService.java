package com.ecommerce.crm.service;

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
            log.error("Redis SET hatası: key = {}", key, e);
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
            log.error("Redis GET hatası: key = {}", key, e);
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.info("Redis DELETE: key = {}", key);
        } catch (Exception e) {
            log.error("Redis DELETE hatası: key = {}", key, e);
        }
    }
}