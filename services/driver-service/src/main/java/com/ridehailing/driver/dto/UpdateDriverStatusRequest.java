package com.ridehailing.driver.dto;

import com.ridehailing.common.dto.DriverStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDriverStatusRequest(
    @NotNull(message = "status is required")
    DriverStatus status
) {
}
