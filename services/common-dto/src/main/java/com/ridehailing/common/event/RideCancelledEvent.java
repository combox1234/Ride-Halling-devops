package com.ridehailing.common.event;

import java.time.Instant;
import java.util.Objects;

public record RideCancelledEvent(
    Long rideId,
    Long passengerId,
    Long driverId,
    String reason,
    Instant cancelledAt
) {
    public RideCancelledEvent {
        Objects.requireNonNull(rideId, "rideId must not be null");
        Objects.requireNonNull(passengerId, "passengerId must not be null");
        Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
    }
}
