package com.ridehailing.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DriverPerformanceResponse(
    Long driverId,
    LocalDate reportDate,
    int completedTrips,
    BigDecimal totalEarnings
) {
}
