package com.ridehailing.passenger.repository;

import com.ridehailing.passenger.entity.PassengerEntity;
import com.ridehailing.passenger.model.PassengerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PassengerRepositoryTest {

    @Autowired
    private SpringDataPassengerRepository springDataPassengerRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @BeforeEach
    void setUp() {
        passengerRepository.clear();
    }

    @Test
    @DisplayName("Should persist and find PassengerEntity via Spring Data JPA and Flyway schema")
    void shouldPersistAndFindPassengerEntity() {
        PassengerEntity entity = new PassengerEntity(
            null,
            101L,
            "John Doe",
            "john.doe@example.com",
            "+1-555-0101",
            4.95,
            12,
            Instant.now()
        );

        PassengerEntity saved = springDataPassengerRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(101L);

        Optional<PassengerEntity> found = springDataPassengerRepository.findByUserId(101L);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getRating()).isEqualTo(4.95);
        assertThat(found.get().getTotalRides()).isEqualTo(12);
    }

    @Test
    @DisplayName("PassengerRepository should persist and query PassengerProfile across relational queries")
    void shouldPersistAndQueryProfileViaRepository() {
        PassengerProfile profile = new PassengerProfile(
            null,
            102L,
            "Jane Smith",
            "jane.smith@example.com",
            "+1-555-0102",
            5.0,
            0,
            Instant.now()
        );

        PassengerProfile saved = passengerRepository.save(profile);

        assertThat(saved.id()).isNotNull();
        assertThat(passengerRepository.existsByUserId(102L)).isTrue();
        assertThat(passengerRepository.existsByEmail("jane.smith@example.com")).isTrue();
        assertThat(passengerRepository.findByUserId(102L)).isPresent();
        assertThat(passengerRepository.findById(saved.id())).isPresent();

        // Test update
        PassengerProfile updated = saved.withUpdatedInfo("Jane Updated", "+1-555-9999");
        PassengerProfile persistedUpdate = passengerRepository.save(updated);
        assertThat(persistedUpdate.name()).isEqualTo("Jane Updated");
        assertThat(persistedUpdate.phone()).isEqualTo("+1-555-9999");
    }
}
