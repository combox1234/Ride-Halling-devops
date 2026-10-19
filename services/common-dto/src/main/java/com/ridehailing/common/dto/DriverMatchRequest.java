package com.ridehailing.common.dto;

import java.util.Objects;

public record DriverMatchRequest(
    Long rideId,
    LocationDto pickupLocation,
    Double radiusKm
) {
    public DriverMatchRequest {
        Objects.requireNonNull(rideId, "rideId must not be null");
        Objects.requireNonNull(pickupLocation, "pickupLocation must not be null");
        if (radiusKm == null || radiusKm <= 0.0) {
            radiusKm = 5.0; // default 5km search radius
        }
    }

    public DriverMatchRequest(Long rideId, LocationDto pickupLocation) {
        this(rideId, pickupLocation, 5.0);
    }
}
