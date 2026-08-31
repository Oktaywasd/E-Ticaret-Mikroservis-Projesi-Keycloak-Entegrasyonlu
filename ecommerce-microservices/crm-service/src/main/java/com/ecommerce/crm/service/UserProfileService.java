package com.ecommerce.crm.service;

import com.ecommerce.crm.dto.request.UserProfileUpdateRequest;
import com.ecommerce.crm.dto.request.UserRegisterRequest;
import com.ecommerce.crm.dto.response.UserProfileResponse;
import com.ecommerce.crm.exception.ResourceNotFoundException;
import com.ecommerce.crm.mapper.UserProfileMapper;
import com.ecommerce.crm.model.UserProfile;
import com.ecommerce.crm.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final KeycloakAdminService keycloakAdminService;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    private static final String CACHE_USER_SESSION_PREFIX = "cache:session:user:";

    @Value("${app.cache.session-ttl:86400}")
    private long sessionTtl;

    /**
     * Yeni kullanıcı kaydı açar.
     * Hem Keycloak IAM üzerine hem de PostgreSQL veritabanına eşzamanlı yazar.
     */
    @Transactional
    public UserProfileResponse registerUser(UserRegisterRequest request) {
        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Bu e-posta adresi zaten kullanımda.");
        }

        // 1. Keycloak üzerinde kullanıcı oluştur
        UUID keycloakUserId = keycloakAdminService.createKeycloakUser(request);

        // 2. PostgreSQL veritabanına UserProfile olarak kaydet
        UserProfile userProfile = userProfileMapper.toEntity(request);
        userProfile.setKeycloakUserId(keycloakUserId);

        UserProfile savedProfile = userProfileRepository.save(userProfile);
        UserProfileResponse response = userProfileMapper.toResponse(savedProfile);

        // Oturumu / profili hemen önbelleğe al
        cacheUserSession(keycloakUserId, response);
        return response;
    }

    /**
     * Keycloak Unique ID (sub claim) üzerinden kullanıcının profil bilgilerini getirir.
     * Önce Redis cache kontrol edilir, yoksa PostgreSQL sorgusu yapılır.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByKeycloakUserId(UUID keycloakUserId) {
        String cacheKey = CACHE_USER_SESSION_PREFIX + keycloakUserId.toString();

        Object cachedData = cacheService.get(cacheKey);
        if (cachedData != null) {
            try {
                return objectMapper.convertValue(cachedData, UserProfileResponse.class);
            } catch (Exception e) {
                log.warn("Session cache deserialize edilemedi, DB'den çekiliyor: {}", e.getMessage());
            }
        }

        UserProfile profile = userProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı profili bulunamadı: " + keycloakUserId));

        UserProfileResponse response = userProfileMapper.toResponse(profile);
        cacheUserSession(keycloakUserId, response);
        return response;
    }

    /**
     * Kullanıcı profilini oluşturur (Eğer yoksa).
     */
    @Transactional
    public UserProfileResponse createProfile(UUID keycloakUserId, String email, UserProfileUpdateRequest request) {
        if (userProfileRepository.findByKeycloakUserId(keycloakUserId).isPresent()) {
            throw new IllegalArgumentException("Kullanıcı profili zaten mevcut.");
        }

        UserProfile userProfile = new UserProfile();
        userProfile.setKeycloakUserId(keycloakUserId);
        userProfile.setEmail(email);
        userProfileMapper.updateEntityFromRequest(request, userProfile);

        UserProfile savedProfile = userProfileRepository.save(userProfile);
        UserProfileResponse response = userProfileMapper.toResponse(savedProfile);

        cacheUserSession(keycloakUserId, response);
        return response;
    }

    /**
     * Kullanıcının profil bilgilerini günceller (Cache Invalidation).
     */
    @Transactional
    public UserProfileResponse updateProfile(UUID keycloakUserId, UserProfileUpdateRequest request) {
        UserProfile profile = userProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı profili bulunamadı: " + keycloakUserId));

        userProfileMapper.updateEntityFromRequest(request, profile);
        UserProfile updatedProfile = userProfileRepository.save(profile);
        UserProfileResponse response = userProfileMapper.toResponse(updatedProfile);

        // Güncellenmiş profili cache'e yaz
        cacheUserSession(keycloakUserId, response);
        return response;
    }

    /**
     * Kullanıcı oturumunu/profilini önbellekten siler (Logout).
     */
    public void evictUserSession(UUID keycloakUserId) {
        String cacheKey = CACHE_USER_SESSION_PREFIX + keycloakUserId.toString();
        cacheService.delete(cacheKey);
        log.info("User session/profile cache temizlendi: {}", cacheKey);
    }

    /**
     * Kullanıcı profilini 24 saatlik TTL ile önbelleğe yazar.
     */
    public void cacheUserSession(UUID keycloakUserId, UserProfileResponse profileResponse) {
        String cacheKey = CACHE_USER_SESSION_PREFIX + keycloakUserId.toString();
        cacheService.set(cacheKey, profileResponse, sessionTtl);
    }

    /**
     * Tüm kullanıcı profillerini getirir (Admin kullanımı için).
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllProfiles() {
        return userProfileRepository.findAll()
                .stream()
                .map(userProfileMapper::toResponse)
                .collect(Collectors.toList());
    }
}