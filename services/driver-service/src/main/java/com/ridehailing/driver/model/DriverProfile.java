package com.ridehailing.driver.model;

import com.ridehailing.common.dto.DriverProfileDto;
import com.ridehailing.common.dto.DriverStatus;

import java.time.Instant;

public record DriverProfile(
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
    int totalTrips,
    Instant createdAt
) {
    public DriverProfile withStatus(DriverStatus newStatus) {
        return new DriverProfile(
            this.id,
            this.userId,
            this.name,
            this.email,
            this.phone,
            this.vehicleMake,
            this.vehicleModel,
            this.licensePlate,
            newStatus != null ? newStatus : this.status,
            this.rating,
            this.totalTrips,
            this.createdAt
        );
    }

    public DriverProfile withUpdatedInfo(String newPhone, String newVehicleMake, String newVehicleModel) {
        return new DriverProfile(
            this.id,
            this.userId,
            this.name,
            this.email,
            newPhone != null && !newPhone.isBlank() ? newPhone : this.phone,
            newVehicleMake != null && !newVehicleMake.isBlank() ? newVehicleMake : this.vehicleMake,
            newVehicleModel != null && !newVehicleModel.isBlank() ? newVehicleModel : this.vehicleModel,
            this.licensePlate,
            this.status,
            this.rating,
            this.totalTrips,
            this.createdAt
        );
    }

    public DriverProfile withIncrementedTrips() {
        return new DriverProfile(
            this.id,
            this.userId,
            this.name,
            this.email,
            this.phone,
            this.vehicleMake,
            this.vehicleModel,
            this.licensePlate,
            this.status,
            this.rating,
            this.totalTrips + 1,
            this.createdAt
        );
    }

    public DriverProfileDto toDto() {
        return new DriverProfileDto(
            this.id,
            this.userId,
            this.name,
            this.email,
            this.phone,
            this.vehicleMake,
            this.vehicleModel,
            this.licensePlate,
            this.status,
            this.rating,
            this.totalTrips
        );
    }
}
