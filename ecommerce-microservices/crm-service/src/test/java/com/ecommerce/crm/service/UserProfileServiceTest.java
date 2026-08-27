package com.ecommerce.crm.service;

import com.ecommerce.crm.dto.request.UserProfileUpdateRequest;
import com.ecommerce.crm.dto.request.UserRegisterRequest;
import com.ecommerce.crm.dto.response.UserProfileResponse;
import com.ecommerce.crm.exception.ResourceNotFoundException;
import com.ecommerce.crm.mapper.UserProfileMapper;
import com.ecommerce.crm.model.UserProfile;
import com.ecommerce.crm.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @InjectMocks
    private UserProfileService userProfileService;

    private UUID keycloakUserId;
    private UserProfile userProfile;
    private UserRegisterRequest registerRequest;
    private UserProfileResponse userProfileResponse;
    private UserProfileUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        keycloakUserId = UUID.randomUUID();

        userProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .keycloakUserId(keycloakUserId)
                .email("test@ecommerce.com")
                .firstName("Ahmet")
                .lastName("Yılmaz")
                .phoneNumber("+905551234567")
                .createdAt(LocalDateTime.now())
                .build();

        registerRequest = new UserRegisterRequest();
        registerRequest.setEmail("test@ecommerce.com");
        registerRequest.setFirstName("Ahmet");
        registerRequest.setLastName("Yılmaz");

        // Builder yerine doğrudan nesne ve setter kullanımı:
        userProfileResponse = new UserProfileResponse();
        userProfileResponse.setId(userProfile.getId());
        userProfileResponse.setKeycloakUserId(keycloakUserId);
        userProfileResponse.setEmail("test@ecommerce.com");
        userProfileResponse.setFirstName("Ahmet");
        userProfileResponse.setLastName("Yılmaz");

        updateRequest = new UserProfileUpdateRequest();
        updateRequest.setFirstName("Mehmet");
        updateRequest.setLastName("Demir");
    }

    @Test
    @DisplayName("Başarılı Kullanıcı Kaydı: Keycloak ve PostgreSQL üzerine profil açılmalı")
    void registerUser_WhenValidRequest_ShouldCreateProfile() {
        // Arrange
        when(userProfileRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(keycloakAdminService.createKeycloakUser(registerRequest)).thenReturn(keycloakUserId);
        when(userProfileMapper.toEntity(registerRequest)).thenReturn(userProfile);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(userProfile);
        when(userProfileMapper.toResponse(userProfile)).thenReturn(userProfileResponse);

        // Act
        UserProfileResponse result = userProfileService.registerUser(registerRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@ecommerce.com");
        verify(keycloakAdminService, times(1)).createKeycloakUser(registerRequest);
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Mükerrer E-posta: Kayıtlı e-posta ile kayıt olmaya çalışınca IllegalArgumentException fırlatmalı")
    void registerUser_WhenDuplicateEmail_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(userProfileRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userProfileService.registerUser(registerRequest);
        });

        verify(keycloakAdminService, never()).createKeycloakUser(any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Keycloak Sub Claim ile Profil Getirme: Eşleşen kullanıcı başarıyla dönmeli")
    void getProfileByKeycloakUserId_WhenUserExists_ShouldReturnProfile() {
        // Arrange
        when(userProfileRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(userProfile));
        when(userProfileMapper.toResponse(userProfile)).thenReturn(userProfileResponse);

        // Act
        UserProfileResponse result = userProfileService.getProfileByKeycloakUserId(keycloakUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getKeycloakUserId()).isEqualTo(keycloakUserId);
        verify(userProfileRepository, times(1)).findByKeycloakUserId(keycloakUserId);
    }

    @Test
    @DisplayName("Profil Bulunamadı: Olmayan Keycloak ID sorgulandığında ResourceNotFoundException fırlatmalı")
    void getProfileByKeycloakUserId_WhenNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(userProfileRepository.findByKeycloakUserId(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userProfileService.getProfileByKeycloakUserId(unknownId);
        });
    }

    @Test
    @DisplayName("Profil Güncelleme: Mevcut kullanıcının bilgileri başarıyla güncellenmeli")
    void updateProfile_WhenValidRequest_ShouldUpdateAndReturnProfile() {
        // Arrange
        when(userProfileRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(userProfile));
        when(userProfileRepository.save(userProfile)).thenReturn(userProfile);
        when(userProfileMapper.toResponse(userProfile)).thenReturn(userProfileResponse);

        // Act
        UserProfileResponse result = userProfileService.updateProfile(keycloakUserId, updateRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(userProfileMapper, times(1)).updateEntityFromRequest(updateRequest, userProfile);
        verify(userProfileRepository, times(1)).save(userProfile);
    }
}