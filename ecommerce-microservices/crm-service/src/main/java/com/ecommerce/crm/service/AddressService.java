package com.ecommerce.crm.service;

import com.ecommerce.crm.dto.request.AddressRequest;
import com.ecommerce.crm.dto.response.AddressResponse;
import com.ecommerce.crm.exception.ResourceNotFoundException;
import com.ecommerce.crm.mapper.AddressMapper;
import com.ecommerce.crm.model.Address;
import com.ecommerce.crm.model.UserProfile;
import com.ecommerce.crm.repository.AddressRepository;
import com.ecommerce.crm.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserProfileRepository userProfileRepository;
    private final AddressMapper addressMapper;

    /**
     * Adres ID'sine göre tek bir adres getirir.
     */
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Adres bulunamadı. ID: " + addressId));
        return addressMapper.toResponse(address);
    }

    /**
     * Kullanıcının hesabına yeni bir adres bağlar ve kaydeder.
     */
    @Transactional
    public AddressResponse addAddress(UUID keycloakUserId, AddressRequest request) {
        UserProfile profile = userProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Adres eklenecek kullanıcı profili bulunamadı: " + keycloakUserId));

        Address address = addressMapper.toEntity(request);
        address.setUserProfile(profile);

        Address savedAddress = addressRepository.save(address);
        return addressMapper.toResponse(savedAddress);
    }

    /**
     * Kullanıcıya ait tüm adresleri getirir.
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(UUID keycloakUserId) {
        List<Address> addresses = addressRepository.findByUserProfileKeycloakUserId(keycloakUserId);
        return addressMapper.toResponseList(addresses);
    }

    /**
     * Kullanıcıya ait adresi günceller.
     */
    @Transactional
    public AddressResponse updateAddress(UUID keycloakUserId, UUID addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserProfileKeycloakUserId(addressId, keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Güncellenecek adres bulunamadı veya bu kullanıcıya ait değil. ID: " + addressId));

        addressMapper.updateEntityFromRequest(request, address);
        Address updatedAddress = addressRepository.save(address);
        return addressMapper.toResponse(updatedAddress);
    }

    /**
     * Kullanıcıya ait adresi siler.
     */
    @Transactional
    public void deleteAddress(UUID keycloakUserId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserProfileKeycloakUserId(addressId, keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Silinecek adres bulunamadı veya bu kullanıcıya ait değil. ID: " + addressId));

        addressRepository.delete(address);
    }
}