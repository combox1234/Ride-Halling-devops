package com.ridehailing.billing.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceResponse(
    Long invoiceId,
    String invoiceNumber,
    Long paymentTransactionId,
    Long rideId,
    Long passengerId,
    BigDecimal amount,
    String currency,
    Instant issuedAt
) {
}
