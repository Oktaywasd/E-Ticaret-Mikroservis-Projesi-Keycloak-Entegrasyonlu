package com.ecommerce.crm.controller;

import com.ecommerce.crm.config.SecurityConfig;
import com.ecommerce.crm.dto.request.AddressRequest;
import com.ecommerce.crm.dto.response.AddressResponse;
import com.ecommerce.crm.service.AddressService;
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

@WebMvcTest(AddressController.class)
@Import(SecurityConfig.class)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AddressService addressService;

    private UUID keycloakUserId;
    private UUID addressId;
    private AddressRequest validAddressRequest;
    private AddressResponse addressResponse;

    @BeforeEach
    void setUp() {
        keycloakUserId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        validAddressRequest = AddressRequest.builder()
                .title("Ev")
                .street("Atatürk Cad. No:5")
                .city("Edirne")
                .state("Merkez")
                .zipCode("22000")
                .country("Türkiye")
                .build();

        addressResponse = new AddressResponse();
        addressResponse.setId(addressId);
        addressResponse.setAddressTitle("Ev");
        addressResponse.setAddressLine("Atatürk Cad. No:5");
        addressResponse.setCity("Edirne");
        addressResponse.setDistrict("Merkez");
        addressResponse.setZipCode("22000");
        addressResponse.setCountry("Türkiye");
    }

    @Test
    @DisplayName("Güvenlik: Token'sız istek yapıldığında 401 Unauthorized dönmeli")
    void getMyAddresses_WhenAnonymousUser_ShouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/addresses"))
                .andExpect(status().isUnauthorized());

        verify(addressService, never()).getUserAddresses(any());
    }

    @Test
    @DisplayName("Başarılı Adres Ekleme: Geçerli JWT ve DTO ile 201 Created dönmeli")
    void addAddress_WhenValidRequest_ShouldReturn201Created() throws Exception {
        when(addressService.addAddress(eq(keycloakUserId), any(AddressRequest.class)))
                .thenReturn(addressResponse);

        mockMvc.perform(post("/api/v1/addresses")
                        .with(jwt().jwt(jwt -> jwt.subject(keycloakUserId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAddressRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(addressId.toString()))
                .andExpect(jsonPath("$.addressTitle").value("Ev"));

        verify(addressService, times(1)).addAddress(eq(keycloakUserId), any(AddressRequest.class));
    }

    @Test
    @DisplayName("DTO Validasyonu: Boş request gönderildiğinde 400 Bad Request dönmeli")
    void addAddress_WhenInvalidDto_ShouldReturn400BadRequest() throws Exception {
        AddressRequest emptyRequest = new AddressRequest();

        mockMvc.perform(post("/api/v1/addresses")
                        .with(jwt().jwt(jwt -> jwt.subject(keycloakUserId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).addAddress(any(), any());
    }

    @Test
    @DisplayName("Kendi Adreslerimi Listeleme: Giriş yapmış kullanıcı adreslerini 200 OK ile listelemeli")
    void getMyAddresses_WhenAuthenticatedUser_ShouldReturn200Ok() throws Exception {
        when(addressService.getUserAddresses(keycloakUserId)).thenReturn(List.of(addressResponse));

        mockMvc.perform(get("/api/v1/addresses")
                        .with(jwt().jwt(jwt -> jwt.subject(keycloakUserId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].addressTitle").value("Ev"));

        verify(addressService, times(1)).getUserAddresses(keycloakUserId);
    }

    @Test
    @DisplayName("Adres Detayı Getirme: Yetkili kullanıcı addressId ile 200 OK alabilmeli")
    void getAddressById_WhenAuthenticatedUser_ShouldReturn200Ok() throws Exception {
        when(addressService.getAddressById(addressId)).thenReturn(addressResponse);

        mockMvc.perform(get("/api/v1/addresses/" + addressId)
                        .with(jwt().jwt(jwt -> jwt.subject(keycloakUserId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(addressId.toString()));

        verify(addressService, times(1)).getAddressById(addressId);
    }

    @Test
    @DisplayName("Adres Silme: Giriş yapmış kullanıcı kendi adresini 204 No Content ile silebilmeli")
    void deleteAddress_WhenAuthenticatedUser_ShouldReturn204NoContent() throws Exception {
        doNothing().when(addressService).deleteAddress(keycloakUserId, addressId);

        mockMvc.perform(delete("/api/v1/addresses/" + addressId)
                        .with(jwt().jwt(jwt -> jwt.subject(keycloakUserId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isNoContent());

        verify(addressService, times(1)).deleteAddress(keycloakUserId, addressId);
    }
}