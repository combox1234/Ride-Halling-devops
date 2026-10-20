package com.ridehailing.reporting.dto;

import java.math.BigDecimal;

public record PlatformOverviewResponse(
    long totalRidesRequested,
    long totalRidesCompleted,
    long totalRidesCancelled,
    BigDecimal totalPlatformRevenue
) {
}
