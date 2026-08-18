package com.ecommerce.crm.service;

import com.ecommerce.crm.dto.request.UserRegisterRequest;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.realm}")
    private String realm;

    public UUID createKeycloakUser(UserRegisterRequest request) {
        RealmResource realmResource = keycloakAdminClient.realm(realm);
        UsersResource usersResource = realmResource.users();

        // 1. Keycloak User Representation Hazırlama
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmailVerified(true);

        // Şifre Tanımlama
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        // 2. Keycloak REST API İsteği
        Response response = usersResource.create(user);

        if (response.getStatus() != 201) {
            log.error("Keycloak kullanıcı oluşturulamadı. Status: {}", response.getStatus());
            throw new RuntimeException("Keycloak üzerinde kullanıcı oluşturulamadı. Hata Kodu: " + response.getStatus());
        }

        // 3. Oluşturulan Kullanıcının ID'sini (UUID) Alma
        String path = response.getLocation().getPath();
        String userIdStr = path.substring(path.lastIndexOf("/") + 1);
        UUID userId = UUID.fromString(userIdStr);

        // 4. Varsayılan Rolü (ROLE_CUSTOMER) Atama
        try {
            RoleRepresentation customerRole = realmResource.roles().get("ROLE_CUSTOMER").toRepresentation();
            usersResource.get(userIdStr).roles().realmLevel().add(Collections.singletonList(customerRole));
        } catch (Exception e) {
            log.warn("ROLE_CUSTOMER rolü Keycloak üzerinde bulunamadı veya atanamadı: {}", e.getMessage());
        }

        log.info("Keycloak kullanıcısı başarıyla oluşturuldu. UserId: {}", userId);
        return userId;
    }
}