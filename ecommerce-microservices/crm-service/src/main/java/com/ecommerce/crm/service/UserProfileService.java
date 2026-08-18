package com.ecommerce.crm.service;

import com.ecommerce.crm.dto.request.UserProfileUpdateRequest;
import com.ecommerce.crm.dto.request.UserRegisterRequest;
import com.ecommerce.crm.dto.response.UserProfileResponse;
import com.ecommerce.crm.exception.ResourceNotFoundException;
import com.ecommerce.crm.mapper.UserProfileMapper;
import com.ecommerce.crm.model.UserProfile;
import com.ecommerce.crm.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final KeycloakAdminService keycloakAdminService;

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
        return userProfileMapper.toResponse(savedProfile);
    }

    /**
     * Keycloak Unique ID (sub claim) üzerinden kullanıcının profil bilgilerini getirir.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByKeycloakUserId(UUID keycloakUserId) {
        UserProfile profile = userProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı profili bulunamadı: " + keycloakUserId));
        return userProfileMapper.toResponse(profile);
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
        return userProfileMapper.toResponse(savedProfile);
    }

    /**
     * Kullanıcının profil bilgilerini günceller.
     */
    @Transactional
    public UserProfileResponse updateProfile(UUID keycloakUserId, UserProfileUpdateRequest request) {
        UserProfile profile = userProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı profili bulunamadı: " + keycloakUserId));

        userProfileMapper.updateEntityFromRequest(request, profile);
        UserProfile updatedProfile = userProfileRepository.save(profile);
        return userProfileMapper.toResponse(updatedProfile);
    }
}