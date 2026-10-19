package com.ridehailing.ride.model;

import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.common.dto.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RideRecord(
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
    public RideRecord withStatus(RideStatus newStatus) {
        return new RideRecord(
            this.id,
            this.passengerId,
            this.driverId,
            this.pickupLocation,
            this.dropoffLocation,
            this.estimatedFare,
            this.finalFare,
            newStatus,
            this.idempotencyKey,
            this.requestedAt,
            this.completedAt
        );
    }

    public RideRecord withStatusAndDriver(RideStatus newStatus, Long driverId) {
        return new RideRecord(
            this.id,
            this.passengerId,
            driverId != null ? driverId : this.driverId,
            this.pickupLocation,
            this.dropoffLocation,
            this.estimatedFare,
            this.finalFare,
            newStatus,
            this.idempotencyKey,
            this.requestedAt,
            this.completedAt
        );
    }

    public RideRecord withCompleted(BigDecimal finalFare, String idempotencyKey, Instant completedAt) {
        return new RideRecord(
            this.id,
            this.passengerId,
            this.driverId,
            this.pickupLocation,
            this.dropoffLocation,
            this.estimatedFare,
            finalFare,
            RideStatus.COMPLETED,
            idempotencyKey,
            this.requestedAt,
            completedAt
        );
    }

    public RideRecord withCancelled() {
        return new RideRecord(
            this.id,
            this.passengerId,
            this.driverId,
            this.pickupLocation,
            this.dropoffLocation,
            this.estimatedFare,
            this.finalFare,
            RideStatus.CANCELLED,
            this.idempotencyKey,
            this.requestedAt,
            this.completedAt
        );
    }
}
