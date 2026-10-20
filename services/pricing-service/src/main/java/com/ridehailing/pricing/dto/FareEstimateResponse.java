package com.ridehailing.pricing.dto;

import java.math.BigDecimal;

public record FareEstimateResponse(
    String zoneCode,
    String zoneName,
    BigDecimal baseFare,
    double distanceKm,
    BigDecimal perKmRate,
    BigDecimal distanceCharge,
    double durationMinutes,
    BigDecimal perMinuteRate,
    BigDecimal durationCharge,
    BigDecimal surgeMultiplier,
    boolean minimumFareApplied,
    BigDecimal estimatedFare,
    String currency
) {
}
