package com.ridehailing.billing.dto;

import com.ridehailing.common.dto.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentReceiptResponse(
    Long paymentId,
    Long rideId,
    Long passengerId,
    BigDecimal amount,
    PaymentStatus status,
    String paymentMethod,
    String transactionRef,
    String idempotencyKey,
    String errorCode,
    String errorMessage,
    String invoiceNumber,
    Instant processedAt
) {
}
