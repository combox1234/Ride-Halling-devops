package com.ridehailing.common.dto;

import java.util.Objects;

public record DriverProfileDto(
    Long id,
    Long userId,
    String name,
    String email,
    String phone,
    String vehicleMake,
    String vehicleModel,
    String licensePlate,
    DriverStatus status,
    Double rating,
    Integer totalTrips
) {
    public DriverProfileDto {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(phone, "phone must not be null");
        Objects.requireNonNull(vehicleMake, "vehicleMake must not be null");
        Objects.requireNonNull(vehicleModel, "vehicleModel must not be null");
        Objects.requireNonNull(licensePlate, "licensePlate must not be null");
        if (status == null) {
            status = DriverStatus.OFFLINE;
        }
        if (rating == null) {
            rating = 5.0;
        }
        if (totalTrips == null) {
            totalTrips = 0;
        }
    }
}
