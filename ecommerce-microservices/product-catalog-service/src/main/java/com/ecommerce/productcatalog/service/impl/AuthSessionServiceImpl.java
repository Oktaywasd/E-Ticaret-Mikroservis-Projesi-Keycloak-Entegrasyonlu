package com.ecommerce.productcatalog.service.impl;

import com.ecommerce.productcatalog.dto.SessionCacheDto;
import com.ecommerce.productcatalog.service.AuthSessionService;
import com.ecommerce.productcatalog.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSessionServiceImpl implements AuthSessionService {

    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    private static final String SESSION_CACHE_PREFIX = "cache:session:user:";

    @Value("${app.cache.session-ttl:86400}") // 24 Saat varsayılan TTL
    private long defaultSessionTtl;

    @Override
    public void cacheUserSession(SessionCacheDto sessionDto, long ttlInSeconds) {
        long ttl = ttlInSeconds > 0 ? ttlInSeconds : defaultSessionTtl;
        String key = SESSION_CACHE_PREFIX + sessionDto.getUserId();
        cacheService.set(key, sessionDto, ttl);
        log.info("Kullanıcı oturumu Redis'e yazıldı: {} (TTL: {}s)", sessionDto.getUserId(), ttl);
    }

    @Override
    public SessionCacheDto getSession(String userId) {
        String key = SESSION_CACHE_PREFIX + userId;
        Object cached = cacheService.get(key);
        if (cached == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(cached, SessionCacheDto.class);
        } catch (Exception e) {
            log.warn("Session cache verisi dönüştürülemedi: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isSessionValid(String userId, String token) {
        SessionCacheDto session = getSession(userId);
        if (session == null || !session.isActive()) {
            return false;
        }
        return session.getToken() == null || session.getToken().equals(token);
    }

    @Override
    public void invalidateSession(String userId) {
        String key = SESSION_CACHE_PREFIX + userId;
        cacheService.delete(key);
        log.info("Kullanıcı oturumu Redis'ten silindi (Revoked): {}", userId);
    }
}