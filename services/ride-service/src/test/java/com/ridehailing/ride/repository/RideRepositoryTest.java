package com.ridehailing.ride.repository;

import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.common.dto.RideStatus;
import com.ridehailing.ride.entity.RideEntity;
import com.ridehailing.ride.model.RideRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RideRepositoryTest {

    @Autowired
    private SpringDataRideRepository springDataRideRepository;

    @Autowired
    private RideRepository rideRepository;

    @BeforeEach
    void setUp() {
        rideRepository.clear();
    }

    @Test
    @DisplayName("Should persist and find RideEntity via Spring Data JPA and Flyway schema")
    void shouldPersistAndFindRideEntity() {
        RideEntity entity = new RideEntity(
            null,
            101L,
            201L,
            37.7749,
            -122.4194,
            "123 Market St",
            37.7849,
            -122.4094,
            "456 Mission St",
            new BigDecimal("15.50"),
            null,
            RideStatus.DRIVER_ASSIGNED,
            null,
            Instant.now(),
            null
        );

        RideEntity saved = springDataRideRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPassengerId()).isEqualTo(101L);
        assertThat(saved.getVersion()).isNotNull();

        Optional<RideEntity> found = springDataRideRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEstimatedFare()).isEqualByComparingTo("15.50");
        assertThat(found.get().getStatus()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
    }

    @Test
    @DisplayName("RideRepository should support atomic state transition and idempotency key lookup")
    void shouldUpdateAtomicallyAndFindByIdempotencyKey() {
        LocationDto pickup = new LocationDto(40.7128, -74.0060, "NYC Pickup");
        LocationDto dropoff = new LocationDto(40.7589, -73.9851, "Times Square");

        RideRecord initial = new RideRecord(
            null,
            202L,
            null,
            pickup,
            dropoff,
            new BigDecimal("22.00"),
            null,
            RideStatus.REQUESTED,
            null,
            Instant.now(),
            null
        );

        RideRecord saved = rideRepository.save(initial);
        assertThat(saved.id()).isNotNull();

        // Perform atomic update to COMPLETED with idempotency key
        String idempotencyKey = "PAY-RIDE-" + saved.id();
        Instant completedAt = Instant.now();

        Optional<RideRecord> updated = rideRepository.updateAtomically(saved.id(), current ->
            current.withCompleted(new BigDecimal("22.00"), idempotencyKey, completedAt)
        );

        assertThat(updated).isPresent();
        assertThat(updated.get().status()).isEqualTo(RideStatus.COMPLETED);
        assertThat(updated.get().idempotencyKey()).isEqualTo(idempotencyKey);

        // Find by idempotency key
        Optional<RideRecord> foundByKey = rideRepository.findByIdempotencyKey(idempotencyKey);
        assertThat(foundByKey).isPresent();
        assertThat(foundByKey.get().id()).isEqualTo(saved.id());
    }

    @Test
    @DisplayName("Should detect concurrent modification via optimistic locking")
    void shouldDetectOptimisticLockingFailure() {
        RideEntity entity = new RideEntity(
            null,
            303L,
            null,
            34.0522,
            -118.2437,
            "LA Pickup",
            34.0736,
            -118.4004,
            "Beverly Hills",
            new BigDecimal("30.00"),
            null,
            RideStatus.REQUESTED,
            null,
            Instant.now(),
            null
        );

        RideEntity saved = springDataRideRepository.saveAndFlush(entity);

        // Load two independent instances
        RideEntity firstRef = springDataRideRepository.findById(saved.getId()).orElseThrow();
        RideEntity secondRef = springDataRideRepository.findById(saved.getId()).orElseThrow();

        // First update succeeds and increments version
        firstRef.setStatus(RideStatus.DRIVER_ASSIGNED);
        springDataRideRepository.saveAndFlush(firstRef);

        // Second update with stale version must trigger OptimisticLockingFailureException
        secondRef.setStatus(RideStatus.CANCELLED);
        assertThatThrownBy(() -> springDataRideRepository.saveAndFlush(secondRef))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
