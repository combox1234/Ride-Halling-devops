package com.ridehailing.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record RideCompletedEvent(
    Long rideId,
    Long passengerId,
    Long driverId,
    BigDecimal finalFare,
    String idempotencyKey,
    Instant completedAt
) {
    public RideCompletedEvent {
        Objects.requireNonNull(rideId, "rideId must not be null");
        Objects.requireNonNull(passengerId, "passengerId must not be null");
        Objects.requireNonNull(driverId, "driverId must not be null");
        Objects.requireNonNull(finalFare, "finalFare must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        if (finalFare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("finalFare cannot be negative");
        }
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
