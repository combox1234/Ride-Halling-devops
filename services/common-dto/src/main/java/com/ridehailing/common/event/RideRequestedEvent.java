package com.ridehailing.common.event;

import com.ridehailing.common.dto.LocationDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record RideRequestedEvent(
    Long rideId,
    Long passengerId,
    LocationDto pickupLocation,
    LocationDto dropoffLocation,
    BigDecimal estimatedFare,
    Instant requestedAt
) {
    public RideRequestedEvent {
        Objects.requireNonNull(rideId, "rideId must not be null");
        Objects.requireNonNull(passengerId, "passengerId must not be null");
        Objects.requireNonNull(pickupLocation, "pickupLocation must not be null");
        Objects.requireNonNull(dropoffLocation, "dropoffLocation must not be null");
        Objects.requireNonNull(estimatedFare, "estimatedFare must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        if (estimatedFare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("estimatedFare cannot be negative");
        }
    }
}
