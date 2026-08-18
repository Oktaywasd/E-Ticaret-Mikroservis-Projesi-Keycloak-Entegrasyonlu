package com.ecommerce.crm.dto.request;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
}