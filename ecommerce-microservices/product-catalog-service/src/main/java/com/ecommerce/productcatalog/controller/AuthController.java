package com.ecommerce.productcatalog.controller;

import com.ecommerce.productcatalog.dto.SessionCacheDto;
import com.ecommerce.productcatalog.service.AuthSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth & Session Controller", description = "Redis Session Management Endpoints")
public class AuthController {

    private final AuthSessionService authSessionService;

    @PostMapping("/cache-session")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Kullanıcı giriş yaptıktan sonra oturumu Redis'e kaydet (24 Saat)")
    public ResponseEntity<Void> cacheSession(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.badRequest().build();
        }

        SessionCacheDto sessionDto = SessionCacheDto.builder()
                .userId(jwt.getSubject())
                .username(jwt.getClaimAsString("preferred_username"))
                .email(jwt.getClaimAsString("email"))
                .token(jwt.getTokenValue())
                .loginTime(LocalDateTime.now())
                .active(true)
                .build();

        authSessionService.cacheUserSession(sessionDto, 86400L);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate-session")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Redis üzerinden aktif oturumu kontrol et")
    public ResponseEntity<SessionCacheDto> validateSession(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        SessionCacheDto session = authSessionService.getSession(jwt.getSubject());
        if (session == null || !session.isActive()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(session);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Çıkış yapıldığında oturumu Redis'ten sil (Revoke)")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            authSessionService.invalidateSession(jwt.getSubject());
        }
        return ResponseEntity.noContent().build();
    }
}