package com.ridehailing.common.dto;

import java.util.Objects;

public record DriverLocationPing(
    Long driverId,
    Double latitude,
    Double longitude
) {
    public DriverLocationPing {
        Objects.requireNonNull(driverId, "driverId must not be null");
        Objects.requireNonNull(latitude, "latitude must not be null");
        Objects.requireNonNull(longitude, "longitude must not be null");
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }
    }
}
