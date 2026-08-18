package com.ecommerce.crm.controller;

import com.ecommerce.crm.dto.request.AddressRequest;
import com.ecommerce.crm.dto.response.AddressResponse;
import com.ecommerce.crm.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Address Controller", description = "Kullanıcı adres yönetimi uç noktaları")
public class AddressController {

    private final AddressService addressService;

    /**
     * ID'ye göre tek bir adresi getirir (Order Service vb. Feign istemcileri için).
     */
    @GetMapping("/{addressId}")
    @Operation(summary = "ID ile Adres Detayı Getir", description = "Belirtilen addressId'ye ait adres bilgilerini döndürür.")
    public ResponseEntity<AddressResponse> getAddressById(@PathVariable UUID addressId) {
        AddressResponse response = addressService.getAddressById(addressId);
        return ResponseEntity.ok(response);
    }

    /**
     * Giriş yapmış kullanıcıya yeni bir adres ekler.
     */
    @PostMapping
    @Operation(summary = "Yeni Adres Ekle", description = "Giriş yapmış kullanıcının hesabına yeni adres kaydeder.")
    public ResponseEntity<AddressResponse> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddressRequest request) {

        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        AddressResponse response = addressService.addAddress(keycloakUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Giriş yapmış kullanıcının kayıtlı tüm adreslerini getirir.
     */
    @GetMapping
    @Operation(summary = "Adreslerimi Listele", description = "Giriş yapmış kullanıcının tüm kayıtlı adreslerini listeler.")
    public ResponseEntity<List<AddressResponse>> getMyAddresses(@AuthenticationPrincipal Jwt jwt) {

        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        List<AddressResponse> response = addressService.getUserAddresses(keycloakUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Giriş yapmış kullanıcının seçilen bir adresini kısmi olarak günceller.
     */
    @PatchMapping("/{addressId}")
    @Operation(summary = "Adres Güncelle", description = "Kullanıcıya ait bir adresin bilgilerini günceller.")
    public ResponseEntity<AddressResponse> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request) {

        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        AddressResponse response = addressService.updateAddress(keycloakUserId, addressId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Giriş yapmış kullanıcının seçilen adresini siler.
     */
    @DeleteMapping("/{addressId}")
    @Operation(summary = "Adres Sil", description = "Kullanıcıya ait belirtilen ID'deki adresi sistemden siler.")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId) {

        UUID keycloakUserId = UUID.fromString(jwt.getSubject());
        addressService.deleteAddress(keycloakUserId, addressId);
        return ResponseEntity.noContent().build();
    }
}