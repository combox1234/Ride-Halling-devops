package com.ridehailing.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationDtoTest {

    @Test
    @DisplayName("Should create valid LocationDto with coordinates and address")
    void shouldCreateValidLocationDto() {
        LocationDto location = new LocationDto(12.9716, 77.5946, "Bangalore, India");

        assertThat(location.latitude()).isEqualTo(12.9716);
        assertThat(location.longitude()).isEqualTo(77.5946);
        assertThat(location.address()).isEqualTo("Bangalore, India");
    }

    @Test
    @DisplayName("Should accept valid LocationDto with null address")
    void shouldAcceptNullAddress() {
        LocationDto location = new LocationDto(0.0, 0.0, null);

        assertThat(location.latitude()).isEqualTo(0.0);
        assertThat(location.longitude()).isEqualTo(0.0);
        assertThat(location.address()).isNull();
    }

    @Test
    @DisplayName("Should reject null coordinates")
    void shouldRejectNullCoordinates() {
        assertThatThrownBy(() -> new LocationDto(null, 77.5946, "Address"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("latitude");

        assertThatThrownBy(() -> new LocationDto(12.9716, null, "Address"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("longitude");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-90.001, 90.001, -100.0, 100.0})
    @DisplayName("Should reject out-of-bounds latitude values")
    void shouldRejectOutOfBoundsLatitude(double invalidLat) {
        assertThatThrownBy(() -> new LocationDto(invalidLat, 0.0, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Latitude must be between -90 and 90 degrees");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-180.001, 180.001, -200.0, 200.0})
    @DisplayName("Should reject out-of-bounds longitude values")
    void shouldRejectOutOfBoundsLongitude(double invalidLng) {
        assertThatThrownBy(() -> new LocationDto(0.0, invalidLng, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Longitude must be between -180 and 180 degrees");
    }
}
