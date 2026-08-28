package com.ecommerce.crm.controller;

import com.ecommerce.crm.config.SecurityConfig;
import com.ecommerce.crm.dto.request.UserProfileUpdateRequest;
import com.ecommerce.crm.dto.response.UserProfileResponse;
import com.ecommerce.crm.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
@Import(SecurityConfig.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileService userProfileService;

    private UUID keycloakUserId;
    private UserProfileResponse profileResponse;
    private UserProfileUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        keycloakUserId = UUID.randomUUID();

        profileResponse = new UserProfileResponse();
        profileResponse.setId(UUID.randomUUID());
        profileResponse.setKeycloakUserId(keycloakUserId);
        profileResponse.setEmail("test@ecommerce.com");
        profileResponse.setFirstName("Ahmet");
        profileResponse.setLastName("Yılmaz");
        profileResponse.setPhoneNumber("+905551234567");

        updateRequest = new UserProfileUpdateRequest();
        updateRequest.setFirstName("Ahmet");
        updateRequest.setLastName("Yılmaz");
        updateRequest.setPhoneNumber("+905551234567");
    }

    @Test
    @DisplayName("Güvenlik: Token'sız istek yapıldığında 401 Unauthorized dönmeli")
    void getMyProfile_WhenAnonymousUser_ShouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me"))
                .andExpect(status().isUnauthorized());

        verify(userProfileService, never()).getProfileByKeycloakUserId(any());
    }

    @Test
    @DisplayName("Kendi Profilimi Getirme: Giriş yapmış kullanıcı sub claim üzerinden 200 OK ile profilini alabilmeli")
    void getMyProfile_WhenAuthenticatedUser_ShouldReturn200Ok() throws Exception {
        when(userProfileService.getProfileByKeycloakUserId(keycloakUserId)).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/profile/me")
                        .with(jwt().jwt(jwt -> jwt.subject(keycloakUserId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakUserId").value(keycloakUserId.toString()))
                .andExpect(jsonPath("$.email").value("test@ecommerce.com"));

        verify(userProfileService, times(1)).getProfileByKeycloakUserId(keycloakUserId);
    }

    @Test
    @DisplayName("Profil Oluşturma: sub ve email claim'leri ile 200 OK dönmeli")
    void createMyProfile_WhenAuthenticatedUser_ShouldReturn200Ok() throws Exception {
        when(userProfileService.createProfile(eq(keycloakUserId), eq("test@ecommerce.com"), any(UserProfileUpdateRequest.class)))
                .thenReturn(profileResponse);

        mockMvc.perform(post("/api/v1/profile")
                        .with(jwt().jwt(jwt -> {
                            jwt.subject(keycloakUserId.toString());
                            jwt.claim("email", "test@ecommerce.com");
                        }).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@ecommerce.com"));

        verify(userProfileService, times(1)).createProfile(eq(keycloakUserId), eq("test@ecommerce.com"), any(UserProfileUpdateRequest.class));
    }

    @Test
    @DisplayName("Profil Güncelleme: Giriş yapmış kullanıcı profilini 200 OK ile güncelleyebilmeli")
    void updateMyProfile_WhenAuthenticatedUser_ShouldReturn200Ok() throws Exception {
        when(userProfileService.updateProfile(eq(keycloakUserId), any(UserProfileUpdateRequest.class)))
                .thenReturn(profileResponse);

        mockMvc.perform(patch("/api/v1/profile/me")
                        .with(jwt().jwt(jwt -> jwt.subject(keycloakUserId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ahmet"));

        verify(userProfileService, times(1)).updateProfile(eq(keycloakUserId), any(UserProfileUpdateRequest.class));
    }

    @Test
    @DisplayName("RBAC Admin Yetkisiz: Normal müşteri /all endpoint'ine istek atarsa 403 Forbidden dönmeli")
    void getAllUsers_WhenCustomerRole_ShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/profile/all")
                        .with(jwt().jwt(jwt -> jwt.subject(keycloakUserId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());

        verify(userProfileService, never()).getAllProfiles();
    }

    @Test
    @DisplayName("RBAC Admin Yetkili: Admin kullanıcısı tüm profilleri 200 OK ile listeleyebilmeli")
    void getAllUsers_WhenAdminRole_ShouldReturn200Ok() throws Exception {
        when(userProfileService.getAllProfiles()).thenReturn(List.of(profileResponse));

        mockMvc.perform(get("/api/v1/profile/all")
                        .with(jwt().jwt(jwt -> jwt.subject(keycloakUserId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(userProfileService, times(1)).getAllProfiles();
    }
}