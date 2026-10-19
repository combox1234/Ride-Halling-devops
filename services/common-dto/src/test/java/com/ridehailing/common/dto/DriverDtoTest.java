package com.ridehailing.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverDtoTest {

    @Test
    @DisplayName("Should validate DriverLocationPing coordinates and require non-null values")
    void shouldValidateDriverLocationPing() {
        DriverLocationPing ping = new DriverLocationPing(10L, 37.7749, -122.4194);
        assertThat(ping.driverId()).isEqualTo(10L);
        assertThat(ping.latitude()).isEqualTo(37.7749);
        assertThat(ping.longitude()).isEqualTo(-122.4194);

        assertThatThrownBy(() -> new DriverLocationPing(null, 37.0, -122.0))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("driverId must not be null");

        assertThatThrownBy(() -> new DriverLocationPing(10L, 95.0, -122.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Latitude must be between -90 and 90");

        assertThatThrownBy(() -> new DriverLocationPing(10L, 37.0, 195.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Longitude must be between -180 and 180");
    }

    @Test
    @DisplayName("Should create DriverProfileDto with defaults and validate required fields")
    void shouldCreateDriverProfileDto() {
        DriverProfileDto profile = new DriverProfileDto(
            1L, 100L, "John Doe", "john@example.com", "+1234567890",
            "Toyota", "Camry", "ABC-1234", null, null, null
        );

        assertThat(profile.status()).isEqualTo(DriverStatus.OFFLINE);
        assertThat(profile.rating()).isEqualTo(5.0);
        assertThat(profile.totalTrips()).isEqualTo(0);

        assertThatThrownBy(() -> new DriverProfileDto(1L, null, "John", "john@example.com", "123", "Toyota", "Camry", "ABC", null, null, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should validate DriverMatchRequest and default radius to 5km")
    void shouldValidateDriverMatchRequest() {
        LocationDto pickup = new LocationDto(37.7749, -122.4194, "Market St");
        DriverMatchRequest request = new DriverMatchRequest(50L, pickup);

        assertThat(request.rideId()).isEqualTo(50L);
        assertThat(request.radiusKm()).isEqualTo(5.0);
        assertThat(request.pickupLocation()).isEqualTo(pickup);

        DriverMatchRequest customRadius = new DriverMatchRequest(50L, pickup, 10.0);
        assertThat(customRadius.radiusKm()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should validate DriverCandidateDto properties")
    void shouldValidateDriverCandidateDto() {
        DriverCandidateDto candidate = new DriverCandidateDto(10L, 1.25, 37.7749, -122.4194, null);
        assertThat(candidate.driverId()).isEqualTo(10L);
        assertThat(candidate.distanceKm()).isEqualTo(1.25);
        assertThat(candidate.rating()).isEqualTo(5.0);

        assertThatThrownBy(() -> new DriverCandidateDto(null, 1.0, 37.0, -122.0, 5.0))
            .isInstanceOf(NullPointerException.class);
    }
}
