package com.ecommerce.crm.repository;

import com.ecommerce.crm.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByKeycloakUserId(UUID keycloakUserId);
    Optional<UserProfile> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByKeycloakUserId(UUID keycloakUserId);
}