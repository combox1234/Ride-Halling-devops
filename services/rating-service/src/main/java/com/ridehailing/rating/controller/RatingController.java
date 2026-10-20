package com.ridehailing.rating.controller;

import com.ridehailing.common.dto.ApiResponse;
import com.ridehailing.rating.dto.RatingResponse;
import com.ridehailing.rating.dto.RatingSummaryResponse;
import com.ridehailing.rating.dto.SubmitRatingRequest;
import com.ridehailing.rating.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RatingResponse>> submitRating(
        @Valid @RequestBody SubmitRatingRequest request
    ) {
        RatingResponse response = ratingService.submitRating(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/drivers/{driverId}/summary")
    public ResponseEntity<ApiResponse<RatingSummaryResponse>> getDriverRatingSummary(
        @PathVariable Long driverId
    ) {
        RatingSummaryResponse summary = ratingService.getRateeSummary(driverId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/passengers/{passengerId}/summary")
    public ResponseEntity<ApiResponse<RatingSummaryResponse>> getPassengerRatingSummary(
        @PathVariable Long passengerId
    ) {
        RatingSummaryResponse summary = ratingService.getRateeSummary(passengerId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/rides/{rideId}")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getRatingsForRide(
        @PathVariable Long rideId
    ) {
        List<RatingResponse> ratings = ratingService.getRatingsByRideId(rideId);
        return ResponseEntity.ok(ApiResponse.success(ratings));
    }
}
