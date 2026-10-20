package com.ridehailing.rating.service;

import com.ridehailing.rating.dto.RatingResponse;
import com.ridehailing.rating.dto.RatingSummaryResponse;
import com.ridehailing.rating.dto.SubmitRatingRequest;
import com.ridehailing.rating.entity.RatingEntity;
import com.ridehailing.rating.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        ratingService = new RatingService(ratingRepository);
    }

    @Test
    void shouldSubmitRatingSuccessfully() {
        SubmitRatingRequest request = new SubmitRatingRequest(
            100L, 200L, "PASSENGER", 300L, 5, "Great driving and clean vehicle!"
        );

        when(ratingRepository.existsByRideIdAndRaterId(100L, 200L)).thenReturn(false);
        when(ratingRepository.save(any(RatingEntity.class))).thenAnswer(i -> i.getArgument(0));

        RatingResponse response = ratingService.submitRating(request);

        assertNotNull(response);
        assertEquals(100L, response.rideId());
        assertEquals(200L, response.raterId());
        assertEquals(300L, response.rateeId());
        assertEquals(5, response.score());
        assertEquals("Great driving and clean vehicle!", response.feedback());
    }

    @Test
    void shouldRejectDuplicateRatingSubmission() {
        SubmitRatingRequest request = new SubmitRatingRequest(
            100L, 200L, "PASSENGER", 300L, 5, "Duplicate feedback"
        );

        when(ratingRepository.existsByRideIdAndRaterId(100L, 200L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> ratingService.submitRating(request));
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidRatingScores() {
        SubmitRatingRequest scoreZero = new SubmitRatingRequest(100L, 200L, "PASSENGER", 300L, 0, "Too low");
        assertThrows(IllegalArgumentException.class, () -> ratingService.submitRating(scoreZero));

        SubmitRatingRequest scoreSix = new SubmitRatingRequest(100L, 200L, "PASSENGER", 300L, 6, "Too high");
        assertThrows(IllegalArgumentException.class, () -> ratingService.submitRating(scoreSix));
    }

    @Test
    void shouldCalculateAccurateRollingAverageAndBreakdown() {
        List<RatingEntity> mockRatings = List.of(
            new RatingEntity(1L, 10L, "PASSENGER", 300L, 5, "Perfect"),
            new RatingEntity(2L, 11L, "PASSENGER", 300L, 4, "Good"),
            new RatingEntity(3L, 12L, "PASSENGER", 300L, 5, "Super"),
            new RatingEntity(4L, 13L, "PASSENGER", 300L, 4, "Nice"),
            new RatingEntity(5L, 14L, "PASSENGER", 300L, 3, "Average")
        );
        when(ratingRepository.findByRateeId(300L)).thenReturn(mockRatings);

        // Sum = 5 + 4 + 5 + 4 + 3 = 21 / 5 = 4.20
        RatingSummaryResponse summary = ratingService.getRateeSummary(300L);

        assertNotNull(summary);
        assertEquals(300L, summary.rateeId());
        assertEquals(4.20, summary.averageRating());
        assertEquals(5, summary.totalRatings());
        assertEquals(2L, summary.scoreBreakdown().get(5));
        assertEquals(2L, summary.scoreBreakdown().get(4));
        assertEquals(1L, summary.scoreBreakdown().get(3));
        assertEquals(0L, summary.scoreBreakdown().get(2));
        assertEquals(0L, summary.scoreBreakdown().get(1));
    }

    @Test
    void shouldReturnDefaultSummaryWhenNoRatingsExist() {
        when(ratingRepository.findByRateeId(999L)).thenReturn(Collections.emptyList());

        RatingSummaryResponse summary = ratingService.getRateeSummary(999L);
        assertNotNull(summary);
        assertEquals(5.0, summary.averageRating());
        assertEquals(0, summary.totalRatings());
    }
}
