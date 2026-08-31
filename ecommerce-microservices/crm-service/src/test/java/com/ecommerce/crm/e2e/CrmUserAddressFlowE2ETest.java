package com.ecommerce.crm.e2e;

import com.ecommerce.crm.model.UserProfile;
import com.ecommerce.crm.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Test verilerini her metottan sonra temizler / rollback eder
class CrmUserAddressFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private UUID testUserUuid;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testUserUuid = UUID.randomUUID();
        testEmail = "user_" + UUID.randomUUID().toString().substring(0, 8) + "@ecommerce.com";

        UserProfile profile = UserProfile.builder()
                .keycloakUserId(testUserUuid)
                .firstName("Ali")
                .lastName("Yılmaz")
                .email(testEmail)
                .phoneNumber("5554443322")
                .build();
        userProfileRepository.save(profile);
    }

    @Test
    @DisplayName("E2E: Giriş yapmış müşteri kendi profil bilgilerini sorgulayabilmeli (GET /api/v1/profile/me)")
    void shouldFetchOwnProfileWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject(testUserUuid.toString())
                                        .claim("email", testEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ali"))
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    @DisplayName("E2E: Token olmadan profile istek atıldığında 401 Unauthorized dönmeli")
    void shouldReturn401WhenAnonymousUserTriesToAccessProfile() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("E2E: ROLE_ADMIN yetkisine sahip kullanıcı tüm kullanıcıları listeleyebilmeli (/api/v1/profile/all)")
    void shouldAllowAdminToFetchAllProfiles() throws Exception {
        mockMvc.perform(get("/api/v1/profile/all")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}