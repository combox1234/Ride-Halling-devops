package com.ridehailing.ride.dto;

import com.ridehailing.common.dto.LocationDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RideBookingRequest(
    @NotNull(message = "passengerId must not be null")
    Long passengerId,

    @NotNull(message = "pickupLocation must not be null")
    @Valid
    LocationDto pickupLocation,

    @NotNull(message = "dropoffLocation must not be null")
    @Valid
    LocationDto dropoffLocation
) {
}
