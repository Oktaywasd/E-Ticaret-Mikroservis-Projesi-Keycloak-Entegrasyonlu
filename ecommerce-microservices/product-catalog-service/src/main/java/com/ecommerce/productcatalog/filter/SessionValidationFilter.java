package com.ecommerce.productcatalog.filter;

import com.ecommerce.productcatalog.dto.SessionCacheDto;
import com.ecommerce.productcatalog.service.AuthSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionValidationFilter extends OncePerRequestFilter {

    private final AuthSessionService authSessionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Herkese açık GET isteklerini (ürün listesi, kategori, reels vb.) filtreleme dışında tut
        if ("GET".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/v1/products") ||
                    path.startsWith("/api/v1/categories") ||
                    path.startsWith("/api/v1/reels") ||
                    path.startsWith("/swagger-ui") ||
                    path.startsWith("/v3/api-docs")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String userId = jwt.getSubject();

            SessionCacheDto session = authSessionService.getSession(userId);

            // Sadece Redis'te açıkça active == false (revoke edilmiş) oturum varsa engelle
            if (session != null && Boolean.FALSE.equals(session.isActive())) {
                log.warn("Oturumu sonlandırılmış (Revoked) kullanıcı isteği engellendi: {}", userId);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Session invalidated. Please login again.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}