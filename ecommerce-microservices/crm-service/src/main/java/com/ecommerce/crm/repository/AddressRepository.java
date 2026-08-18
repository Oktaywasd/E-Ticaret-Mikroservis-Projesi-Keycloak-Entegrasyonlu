package com.ecommerce.crm.repository;

import com.ecommerce.crm.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserProfileKeycloakUserId(UUID keycloakUserId);
    Optional<Address> findByIdAndUserProfileKeycloakUserId(UUID id, UUID keycloakUserId);
}