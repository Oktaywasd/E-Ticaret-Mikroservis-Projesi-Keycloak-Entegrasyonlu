package com.ecommerce.crm.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class AddressResponse {
    private UUID id;
    private String addressTitle;
    private String addressLine;
    private String city;
    private String district;
    private String zipCode;
    private String country;
}