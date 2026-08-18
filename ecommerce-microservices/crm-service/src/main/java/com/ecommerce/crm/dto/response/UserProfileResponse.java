package com.ecommerce.crm.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class UserProfileResponse {
    private UUID id;
    private UUID keycloakUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private List<AddressResponse> addresses;
    private LocalDateTime createdAt;
}