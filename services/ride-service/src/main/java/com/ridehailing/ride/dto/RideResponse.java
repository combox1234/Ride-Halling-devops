package com.ridehailing.ride.dto;

import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.common.dto.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RideResponse(
    Long id,
    Long passengerId,
    Long driverId,
    LocationDto pickupLocation,
    LocationDto dropoffLocation,
    BigDecimal estimatedFare,
    BigDecimal finalFare,
    RideStatus status,
    String idempotencyKey,
    Instant requestedAt,
    Instant completedAt
) {
}
