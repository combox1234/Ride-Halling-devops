package com.ridehailing.pricing.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateSurgeMultiplierRequest(
    @NotNull(message = "surgeMultiplier must not be null")
    @DecimalMin(value = "1.00", message = "surgeMultiplier cannot be less than 1.00")
    @DecimalMax(value = "5.00", message = "surgeMultiplier cannot exceed 5.00")
    BigDecimal surgeMultiplier
) {
}
