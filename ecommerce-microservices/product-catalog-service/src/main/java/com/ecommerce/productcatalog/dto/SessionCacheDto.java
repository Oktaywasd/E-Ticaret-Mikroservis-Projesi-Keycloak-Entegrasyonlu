package com.ecommerce.productcatalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCacheDto implements Serializable {
    private String userId;
    private String username;
    private String email;
    private List<String> roles;
    private String token;
    private LocalDateTime loginTime;
    private boolean active;
}