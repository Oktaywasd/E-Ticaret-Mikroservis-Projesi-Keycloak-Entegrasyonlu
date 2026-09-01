package com.ecommerce.crm.integration;

import com.ecommerce.crm.dto.request.AddressRequest;
import com.ecommerce.crm.mapper.AddressMapper;
import com.ecommerce.crm.model.Address;
import com.ecommerce.crm.model.UserProfile;
import com.ecommerce.crm.repository.AddressRepository;
import com.ecommerce.crm.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AddressControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AddressRepository addressRepository;

    @MockBean
    private UserProfileRepository userProfileRepository;

    @SpyBean
    private AddressMapper addressMapper;

    private final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID ADDRESS_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private UserProfile sampleProfile;
    private Address sampleAddress;
    private AddressRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .keycloakUserId(USER_ID)
                .email("test@ecommerce.com")
                .firstName("Ahmet")
                .lastName("Yıldız")
                .build();

        sampleAddress = Address.builder()
                .id(ADDRESS_ID)
                .addressTitle("Ev")
                .addressLine("Bağdat Caddesi No: 10")
                .city("İstanbul")
                .district("Kadıköy")
                .zipCode("34710")
                .country("Türkiye")
                .userProfile(sampleProfile)
                .build();

        sampleRequest = AddressRequest.builder()
                .title("Ev")
                .street("Bağdat Caddesi No: 10")
                .city("İstanbul")
                .state("Kadıköy")
                .zipCode("34710")
                .country("Türkiye")
                .build();
    }

    @Test
    @DisplayName("Adres Ekleme: Giriş yapmış kullanıcı hesabına adres eklediğinde 201 Created dönmeli")
    void addAddress_WhenAuthenticated_ShouldReturnCreated() throws Exception {
        when(userProfileRepository.findByKeycloakUserId(eq(USER_ID))).thenReturn(Optional.of(sampleProfile));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address a = invocation.getArgument(0);
            a.setId(ADDRESS_ID);
            return a;
        });

        mockMvc.perform(post("/api/v1/addresses")
                        .with(jwt()
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                                        new SimpleGrantedAuthority("USER"),
                                        new SimpleGrantedAuthority("CUSTOMER")
                                )
                                .jwt(builder -> builder
                                        .subject(USER_ID.toString())
                                        .claim("preferred_username", "ahmetyildiz")
                                        .claim("realm_access", Map.of("roles", List.of("USER", "CUSTOMER")))
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ADDRESS_ID.toString()))
                .andExpect(jsonPath("$.city").value("İstanbul"));

        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("Adres Listeleme: Giriş yapmış kullanıcı kendi kayıtlı adreslerini listeleyebilmeli (200 OK)")
    void getMyAddresses_WhenAuthenticated_ShouldReturnAddressList() throws Exception {
        when(addressRepository.findByUserProfileKeycloakUserId(eq(USER_ID))).thenReturn(List.of(sampleAddress));

        mockMvc.perform(get("/api/v1/addresses")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ADDRESS_ID.toString()))
                .andExpect(jsonPath("$[0].city").value("İstanbul"));

        verify(addressRepository).findByUserProfileKeycloakUserId(eq(USER_ID));
    }

    @Test
    @DisplayName("ID ile Adres Detayı: Feign ve genel sorgular için adres ID ile detay dönebilmeli (200 OK)")
    void getAddressById_WhenExists_ShouldReturnAddress() throws Exception {
        when(addressRepository.findById(eq(ADDRESS_ID))).thenReturn(Optional.of(sampleAddress));

        mockMvc.perform(get("/api/v1/addresses/{addressId}", ADDRESS_ID)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ADDRESS_ID.toString()))
                .andExpect(jsonPath("$.city").value("İstanbul"));

        verify(addressRepository).findById(eq(ADDRESS_ID));
    }

    @Test
    @DisplayName("Adres Silme: Kullanıcı kendi adresini silebilmeli (204 No Content)")
    void deleteAddress_WhenOwnsAddress_ShouldReturnNoContent() throws Exception {
        when(addressRepository.findByIdAndUserProfileKeycloakUserId(eq(ADDRESS_ID), eq(USER_ID)))
                .thenReturn(Optional.of(sampleAddress));

        mockMvc.perform(delete("/api/v1/addresses/{addressId}", ADDRESS_ID)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder.subject(USER_ID.toString()))))
                .andExpect(status().isNoContent());

        verify(addressRepository).delete(eq(sampleAddress));
    }

    @Test
    @DisplayName("Kullanıcı İzolasyonu: Başka bir kullanıcının adresini silmeye çalışırsa 404 Not Found dönmeli")
    void deleteAddress_WhenNotOwnsAddress_ShouldReturnNotFound() throws Exception {
        when(addressRepository.findByIdAndUserProfileKeycloakUserId(eq(ADDRESS_ID), eq(OTHER_USER_ID)))
                .thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/addresses/{addressId}", ADDRESS_ID)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder.subject(OTHER_USER_ID.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Güvenlik: Token olmadan adres eklemeye çalışıldığında 401 Unauthorized dönmeli")
    void addAddress_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isUnauthorized());
    }
}