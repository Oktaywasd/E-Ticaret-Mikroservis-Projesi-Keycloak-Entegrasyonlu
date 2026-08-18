package com.ecommerce.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotBlank(message = "Adres başlığı boş olamaz (Örn: Ev, İş)")
    private String title;

    @NotBlank(message = "Sokak/Cadde bilgisi boş olamaz")
    private String street;

    @NotBlank(message = "Şehir bilgisi boş olamaz")
    private String city;

    @NotBlank(message = "Eyalet/İlçe bilgisi boş olamaz")
    private String state;

    @NotBlank(message = "Posta kodu boş olamaz")
    private String zipCode;

    @NotBlank(message = "Ülke bilgisi boş olamaz")
    private String country;
}