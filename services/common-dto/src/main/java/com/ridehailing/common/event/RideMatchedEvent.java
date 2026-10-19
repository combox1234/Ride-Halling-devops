package com.ridehailing.common.event;

import java.time.Instant;
import java.util.Objects;

public record RideMatchedEvent(
    Long rideId,
    Long driverId,
    Long passengerId,
    Instant matchedAt
) {
    public RideMatchedEvent {
        Objects.requireNonNull(rideId, "rideId must not be null");
        Objects.requireNonNull(driverId, "driverId must not be null");
        Objects.requireNonNull(passengerId, "passengerId must not be null");
        Objects.requireNonNull(matchedAt, "matchedAt must not be null");
    }
}
