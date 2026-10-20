package com.ridehailing.billing.gateway;

import java.math.BigDecimal;
import java.util.Objects;

public record PaymentChargeRequest(
    Long rideId,
    Long passengerId,
    BigDecimal amount,
    String currency,
    String idempotencyKey,
    String paymentMethod
) {
    public PaymentChargeRequest {
        Objects.requireNonNull(rideId, "rideId must not be null");
        Objects.requireNonNull(passengerId, "passengerId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            paymentMethod = "MOCK_PAYMENT_METHOD";
        }
    }
}
