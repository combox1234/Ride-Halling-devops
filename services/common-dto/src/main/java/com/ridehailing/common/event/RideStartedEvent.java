package com.ridehailing.common.event;

import java.time.Instant;
import java.util.Objects;

public record RideStartedEvent(
    Long rideId,
    Long driverId,
    Long passengerId,
    Instant startedAt
) {
    public RideStartedEvent {
        Objects.requireNonNull(rideId, "rideId must not be null");
        Objects.requireNonNull(driverId, "driverId must not be null");
        Objects.requireNonNull(passengerId, "passengerId must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
    }
}
