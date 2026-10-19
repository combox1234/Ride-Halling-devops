package com.ridehailing.passenger.model;

import java.time.Instant;

public record PassengerProfile(
    Long id,
    Long userId,
    String name,
    String email,
    String phone,
    Double rating,
    int totalRides,
    Instant createdAt
) {
    public PassengerProfile withUpdatedInfo(String newName, String newPhone) {
        return new PassengerProfile(
            this.id,
            this.userId,
            newName != null && !newName.isBlank() ? newName : this.name,
            this.email,
            newPhone != null && !newPhone.isBlank() ? newPhone : this.phone,
            this.rating,
            this.totalRides,
            this.createdAt
        );
    }

    public PassengerProfile withIncrementedRides() {
        return new PassengerProfile(
            this.id,
            this.userId,
            this.name,
            this.email,
            this.phone,
            this.rating,
            this.totalRides + 1,
            this.createdAt
        );
    }
}
