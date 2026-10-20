package com.ridehailing.pricing.dto;

import java.math.BigDecimal;

public record PricingZoneDto(
    Long id,
    String zoneCode,
    String zoneName,
    BigDecimal baseFare,
    BigDecimal perKmRate,
    BigDecimal perMinuteRate,
    BigDecimal surgeMultiplier,
    BigDecimal minFare,
    boolean active
) {
}
