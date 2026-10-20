package com.ridehailing.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyMetricsResponse(
    LocalDate reportDate,
    int totalRidesRequested,
    int totalRidesStarted,
    int totalRidesCompleted,
    int totalRidesCancelled,
    BigDecimal totalRevenue
) {
}
