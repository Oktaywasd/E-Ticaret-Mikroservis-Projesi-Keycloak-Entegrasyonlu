package com.ecommerce.productcatalog.service;

import com.ecommerce.productcatalog.dto.SessionCacheDto;

public interface AuthSessionService {
    void cacheUserSession(SessionCacheDto sessionDto, long ttlInSeconds);
    SessionCacheDto getSession(String userId);
    boolean isSessionValid(String userId, String token);
    void invalidateSession(String userId);
}