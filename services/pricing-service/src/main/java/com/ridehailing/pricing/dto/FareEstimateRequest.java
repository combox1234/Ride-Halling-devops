package com.ridehailing.pricing.dto;

import com.ridehailing.common.dto.LocationDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record FareEstimateRequest(
    @NotNull(message = "pickupLocation must not be null")
    @Valid
    LocationDto pickupLocation,

    @NotNull(message = "dropoffLocation must not be null")
    @Valid
    LocationDto dropoffLocation,

    String zoneCode,

    Double estimatedDurationMinutes
) {
}
