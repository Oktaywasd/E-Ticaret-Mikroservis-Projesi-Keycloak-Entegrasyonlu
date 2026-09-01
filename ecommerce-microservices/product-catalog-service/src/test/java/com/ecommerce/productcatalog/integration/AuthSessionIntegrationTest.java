package com.ecommerce.productcatalog.integration;

import com.ecommerce.productcatalog.dto.SessionCacheDto;
import com.ecommerce.productcatalog.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CacheService cacheService;

    private final String USER_ID = "user_session_test_01";

    @Test
    @DisplayName("1. Cache Session: Giriş sonrası oturum 24 saatlik TTL ile Redis'e kaydedilmeli")
    void cacheSession_WhenAuthenticated_ShouldSaveToRedis() throws Exception {
        mockMvc.perform(post("/api/v1/auth/cache-session")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(b -> b.subject(USER_ID)
                                        .claim("preferred_username", "testuser")
                                        .claim("realm_access", Map.of("roles", List.of("USER"))))))
                .andExpect(status().isOk());

        verify(cacheService, times(1)).set(eq("cache:session:user:" + USER_ID), any(SessionCacheDto.class), eq(86400L));
    }

    @Test
    @DisplayName("2. Validate Session: Redis'te aktif oturumu olan kullanıcı 200 almalı")
    void validateSession_WhenActiveInRedis_ShouldReturn200() throws Exception {
        SessionCacheDto activeSession = SessionCacheDto.builder()
                .userId(USER_ID)
                .username("testuser")
                .active(true)
                .loginTime(LocalDateTime.now())
                .build();

        when(cacheService.get("cache:session:user:" + USER_ID)).thenReturn(activeSession);

        mockMvc.perform(get("/api/v1/auth/validate-session")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(b -> b.subject(USER_ID)
                                        .claim("realm_access", Map.of("roles", List.of("USER"))))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("3. Logout: Çıkış yapıldığında oturum Redis'ten silinmeli")
    void logout_WhenCalled_ShouldDeleteSessionFromRedis() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(b -> b.subject(USER_ID)
                                        .claim("realm_access", Map.of("roles", List.of("USER"))))))
                .andExpect(status().isNoContent());

        verify(cacheService, times(1)).delete("cache:session:user:" + USER_ID);
    }
}