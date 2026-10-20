package com.ridehailing.rating.dto;

import java.time.Instant;

public record RatingResponse(
    Long id,
    Long rideId,
    Long raterId,
    String raterType,
    Long rateeId,
    int score,
    String feedback,
    Instant createdAt
) {
}
