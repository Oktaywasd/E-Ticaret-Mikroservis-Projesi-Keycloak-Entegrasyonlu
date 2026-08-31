package com.ecommerce.crm.integration;

import com.ecommerce.crm.model.Address;
import com.ecommerce.crm.model.UserProfile;
import com.ecommerce.crm.repository.AddressRepository;
import com.ecommerce.crm.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserProfileRepositoryIntegrationTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Test
    @DisplayName("PostgreSQL: Keycloak sub UUID ile profil ve adres ilişkisi kaydedilip getirilmeli")
    void shouldSaveAndFetchUserProfileWithAddress() {
        UUID keycloakId = UUID.randomUUID();

        UserProfile profile = UserProfile.builder()
                .keycloakUserId(keycloakId)
                .firstName("Test")
                .lastName("Kullanici")
                .email("test@ecommerce.com")
                .phoneNumber("5551112233")
                .build();

        UserProfile savedProfile = userProfileRepository.save(profile);
        assertThat(savedProfile.getId()).isNotNull();

        Address address = Address.builder()
                .addressTitle("Ev Adresi")
                .addressLine("Kötekli Mah. Sıtkı Koçman Cad. No:1")
                .city("Muğla")
                .district("Menteşe")
                .country("Türkiye")
                .zipCode("48000")
                .userProfile(savedProfile)
                .build();

        Address savedAddress = addressRepository.save(address);

        assertThat(savedAddress.getId()).isNotNull();
        assertThat(savedAddress.getUserProfile().getKeycloakUserId()).isEqualTo(keycloakId);
        assertThat(savedAddress.getAddressLine()).isEqualTo("Kötekli Mah. Sıtkı Koçman Cad. No:1");
    }
}