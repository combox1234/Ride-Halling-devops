package com.ridehailing.common.event;

import com.ridehailing.common.dto.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PaymentProcessedEvent(
    Long paymentId,
    Long rideId,
    Long passengerId,
    BigDecimal amount,
    PaymentStatus status,
    String transactionRef,
    String idempotencyKey,
    Instant processedAt
) {
    public PaymentProcessedEvent {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(rideId, "rideId must not be null");
        Objects.requireNonNull(passengerId, "passengerId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(processedAt, "processedAt must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
