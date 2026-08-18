package com.ecommerce.crm.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String serverUrl;

    @Value("${keycloak.admin.realm:master}")
    private String adminRealm;

    @Value("${keycloak.admin.username:admin}")
    private String username;

    @Value("${keycloak.admin.password:admin}")
    private String password;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String clientId;

    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(adminRealm)       // Admin oturumu master realm üzerinden açılır
                .username(username)     // Keycloak admin kullanıcı adı
                .password(password)     // Keycloak admin şifresi
                .clientId(clientId)     // Admin API için varsayılan istemci
                .build();
    }
}