package com.ridehailing.common.dto;

import java.util.Objects;

public record DriverCandidateDto(
    Long driverId,
    Double distanceKm,
    Double latitude,
    Double longitude,
    Double rating
) {
    public DriverCandidateDto {
        Objects.requireNonNull(driverId, "driverId must not be null");
        Objects.requireNonNull(distanceKm, "distanceKm must not be null");
        Objects.requireNonNull(latitude, "latitude must not be null");
        Objects.requireNonNull(longitude, "longitude must not be null");
        if (rating == null) {
            rating = 5.0;
        }
    }
}
