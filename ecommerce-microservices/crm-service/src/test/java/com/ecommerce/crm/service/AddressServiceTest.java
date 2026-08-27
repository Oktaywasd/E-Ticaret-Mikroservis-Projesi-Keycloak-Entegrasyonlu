package com.ecommerce.crm.service;

import com.ecommerce.crm.dto.request.AddressRequest;
import com.ecommerce.crm.dto.response.AddressResponse;
import com.ecommerce.crm.exception.ResourceNotFoundException;
import com.ecommerce.crm.mapper.AddressMapper;
import com.ecommerce.crm.model.Address;
import com.ecommerce.crm.model.UserProfile;
import com.ecommerce.crm.repository.AddressRepository;
import com.ecommerce.crm.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressService addressService;

    private UUID keycloakUserId;
    private UUID otherUserId;
    private UUID addressId;
    private UserProfile userProfile;
    private Address address;
    private AddressRequest addressRequest;
    private AddressResponse addressResponse;

    @BeforeEach
    void setUp() {
        keycloakUserId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        userProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .keycloakUserId(keycloakUserId)
                .email("test@ecommerce.com")
                .build();

        address = Address.builder()
                .id(addressId)
                .addressTitle("Ev")
                .addressLine("Atatürk Cad. No:5")
                .city("Edirne")
                .district("Merkez")
                .zipCode("22000")
                .country("Türkiye")
                .userProfile(userProfile)
                .build();

        addressRequest = AddressRequest.builder()
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
    @DisplayName("Başarılı Adres Ekleme: Adres kullanıcı profiline bağlanarak kaydedilmeli")
    void addAddress_WhenValidRequest_ShouldSaveAddress() {
        // Arrange
        when(userProfileRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(userProfile));
        when(addressMapper.toEntity(addressRequest)).thenReturn(address);
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        // Act
        AddressResponse result = addressService.addAddress(keycloakUserId, addressRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAddressTitle()).isEqualTo("Ev");
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Kullanıcı Adresleri Listeleme: Sadece o kullanıcıya ait adresler listelenmeli")
    void getUserAddresses_WhenCalled_ShouldReturnAddressList() {
        // Arrange
        when(addressRepository.findByUserProfileKeycloakUserId(keycloakUserId)).thenReturn(List.of(address));
        when(addressMapper.toResponseList(List.of(address))).thenReturn(List.of(addressResponse));

        // Act
        List<AddressResponse> result = addressService.getUserAddresses(keycloakUserId);

        // Assert
        assertThat(result).hasSize(1);
        verify(addressRepository, times(1)).findByUserProfileKeycloakUserId(keycloakUserId);
    }

    @Test
    @DisplayName("Kullanıcı İzolasyonu (Adres Güncelleme): Başkasına ait adres güncellenmek istendiğinde ResourceNotFoundException fırlatmalı")
    void updateAddress_WhenAddressNotBelongsToUser_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(addressRepository.findByIdAndUserProfileKeycloakUserId(addressId, otherUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                addressService.updateAddress(otherUserId, addressId, addressRequest)
        );

        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kullanıcı İzolasyonu (Adres Silme): Başkasına ait adres silinmek istendiğinde ResourceNotFoundException fırlatmalı")
    void deleteAddress_WhenAddressNotBelongsToUser_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(addressRepository.findByIdAndUserProfileKeycloakUserId(addressId, otherUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                addressService.deleteAddress(otherUserId, addressId)
        );

        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Başarılı Adres Silme: Kendi adresini silen kullanıcının adresi silinmeli")
    void deleteAddress_WhenValid_ShouldDeleteAddress() {
        // Arrange
        when(addressRepository.findByIdAndUserProfileKeycloakUserId(addressId, keycloakUserId)).thenReturn(Optional.of(address));

        // Act
        addressService.deleteAddress(keycloakUserId, addressId);

        // Assert
        verify(addressRepository, times(1)).delete(address);
    }
}