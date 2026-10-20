package com.ridehailing.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitRatingRequest(
    @NotNull(message = "rideId must not be null")
    Long rideId,

    @NotNull(message = "raterId must not be null")
    Long raterId,

    @NotBlank(message = "raterType must not be blank")
    String raterType,

    @NotNull(message = "rateeId must not be null")
    Long rateeId,

    @Min(value = 1, message = "score must be between 1 and 5")
    @Max(value = 5, message = "score must be between 1 and 5")
    int score,

    @Size(max = 500, message = "feedback cannot exceed 500 characters")
    String feedback
) {
}
