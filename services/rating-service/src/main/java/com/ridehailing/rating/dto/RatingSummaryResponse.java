package com.ridehailing.rating.dto;

import java.util.Map;

public record RatingSummaryResponse(
    Long rateeId,
    double averageRating,
    long totalRatings,
    Map<Integer, Long> scoreBreakdown
) {
}
