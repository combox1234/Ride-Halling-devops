package com.ridehailing.driver.repository;

import com.ridehailing.common.dto.DriverStatus;
import com.ridehailing.driver.model.DriverProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class DriverRepositoryTest {

    @Autowired
    private SpringDataDriverRepository springDataDriverRepository;

    private DriverRepository driverRepository;

    @BeforeEach
    void setUp() {
        driverRepository = new DriverRepository(springDataDriverRepository);
        driverRepository.clear();
    }

    @Test
    @DisplayName("Should persist and retrieve driver profile via JPA")
    void shouldPersistAndRetrieveDriver() {
        DriverProfile profile = new DriverProfile(
            null, 101L, "David Miller", "david@example.com", "+15551234567",
            "Toyota", "Prius", "DRV-101", DriverStatus.OFFLINE, 5.0, 0, Instant.now()
        );

        DriverProfile saved = driverRepository.save(profile);
        assertThat(saved.id()).isNotNull();

        Optional<DriverProfile> found = driverRepository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("David Miller");
        assertThat(found.get().licensePlate()).isEqualTo("DRV-101");
        assertThat(found.get().status()).isEqualTo(DriverStatus.OFFLINE);
    }

    @Test
    @DisplayName("Should enforce unique constraints on userId, email, and license plate")
    void shouldEnforceUniqueConstraints() {
        DriverProfile d1 = new DriverProfile(
            null, 102L, "Alice Brown", "alice@example.com", "+15559876543",
            "Honda", "Civic", "DRV-102", DriverStatus.OFFLINE, 5.0, 0, Instant.now()
        );
        driverRepository.save(d1);

        // Duplicate userId
        DriverProfile duplicateUser = new DriverProfile(
            null, 102L, "Other Alice", "other@example.com", "+15550000000",
            "Ford", "Focus", "DRV-999", DriverStatus.OFFLINE, 5.0, 0, Instant.now()
        );
        assertThatThrownBy(() -> driverRepository.save(duplicateUser))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
