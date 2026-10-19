package com.ridehailing.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiResponseTest {

    @Test
    @DisplayName("Should create successful ApiResponse")
    void shouldCreateSuccessApiResponse() {
        ApiResponse<String> response = ApiResponse.success("Custom Success", "Test Data");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Custom Success");
        assertThat(response.data()).isEqualTo("Test Data");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create failure ApiResponse")
    void shouldCreateFailureApiResponse() {
        ApiResponse<Void> response = ApiResponse.failure("Something went wrong");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Something went wrong");
        assertThat(response.data()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create valid ApiErrorResponse via factory")
    void shouldCreateApiErrorResponse() {
        ApiErrorResponse error = ApiErrorResponse.of(404, "Not Found", "Resource not found", "/api/v1/rides/99");

        assertThat(error.status()).isEqualTo(404);
        assertThat(error.error()).isEqualTo("Not Found");
        assertThat(error.message()).isEqualTo("Resource not found");
        assertThat(error.path()).isEqualTo("/api/v1/rides/99");
        assertThat(error.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should reject null fields in ApiErrorResponse constructor")
    void shouldRejectNullInApiErrorResponse() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> new ApiErrorResponse(null, 500, "Error", "Message", "/path"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("timestamp");

        assertThatThrownBy(() -> new ApiErrorResponse(now, 500, null, "Message", "/path"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("error");

        assertThatThrownBy(() -> new ApiErrorResponse(now, 500, "Error", null, "/path"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("message");
    }
}
