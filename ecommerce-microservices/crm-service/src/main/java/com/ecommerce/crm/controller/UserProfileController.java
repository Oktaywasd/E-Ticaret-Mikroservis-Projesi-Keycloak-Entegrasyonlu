package com.ecommerce.crm.controller;

import com.ecommerce.crm.dto.request.UserProfileUpdateRequest;
import com.ecommerce.crm.dto.response.UserProfileResponse;
import com.ecommerce.crm.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * Giriş yapmış kullanıcının kendi profil bilgilerini getirir.
     * Token içerisindeki 'sub' claim'i üzerinden Keycloak User ID tespit edilir.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        UserProfileResponse response = userProfileService.getProfileByKeycloakUserId(keycloakUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Kullanıcı profilini oluşturur (Eğer yoksa).
     */
    @PostMapping
    public ResponseEntity<UserProfileResponse> createMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        UserProfileResponse response = userProfileService.createProfile(keycloakUserId, email, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Giriş yapmış kullanıcının kendi profil bilgilerini günceller.
     */
    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        UserProfileResponse response = userProfileService.updateProfile(keycloakUserId, request);
        return ResponseEntity.ok(response);
    }
}