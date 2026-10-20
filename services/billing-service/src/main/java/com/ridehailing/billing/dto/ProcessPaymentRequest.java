package com.ridehailing.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProcessPaymentRequest(
    @NotNull(message = "rideId must not be null")
    Long rideId,

    @NotNull(message = "passengerId must not be null")
    Long passengerId,

    @NotNull(message = "amount must not be null")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    BigDecimal amount,

    @NotBlank(message = "idempotencyKey must not be blank")
    String idempotencyKey,

    String paymentMethod
) {
}
