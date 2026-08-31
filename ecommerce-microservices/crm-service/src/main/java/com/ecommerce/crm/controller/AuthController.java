package com.ecommerce.crm.controller;

import com.ecommerce.crm.dto.request.UserRegisterRequest;
import com.ecommerce.crm.dto.response.UserProfileResponse;
import com.ecommerce.crm.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth & Session Management", description = "Kayıt, oturum ve Redis Session Cache API'leri")
public class AuthController {

    private final UserProfileService userProfileService;

    @PostMapping("/register")
    @Operation(summary = "Yeni Kullanıcı Kaydı", description = "Keycloak ve CRM veritabanına eşzamanlı kayıt açar ve oturumu cache'ler.")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        UserProfileResponse response = userProfileService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Oturum Kapat (Logout)", description = "Redis üzerindeki 24 saatlik kullanıcı session/profil cache'ini anında temizler.")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            UUID keycloakUserId = UUID.fromString(jwt.getSubject());
            userProfileService.evictUserSession(keycloakUserId);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate-session")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Oturum Doğrula", description = "Kullanıcının Redis cache üzerindeki aktif profil oturumunu doğrular.")
    public ResponseEntity<Map<String, Object>> validateSession(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        UserProfileResponse profile = userProfileService.getProfileByKeycloakUserId(keycloakUserId);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "userId", keycloakUserId,
                "email", profile.getEmail(),
                "fullName", profile.getFirstName() + " " + profile.getLastName()
        ));
    }
}