package com.ridehailing.rating.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.rating.dto.RatingResponse;
import com.ridehailing.rating.dto.RatingSummaryResponse;
import com.ridehailing.rating.dto.SubmitRatingRequest;
import com.ridehailing.rating.service.RatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RatingController.class)
class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RatingService ratingService;

    @Test
    void shouldSubmitRatingSuccessfully() throws Exception {
        SubmitRatingRequest request = new SubmitRatingRequest(
            101L, 202L, "PASSENGER", 303L, 5, "Awesome ride"
        );
        RatingResponse response = new RatingResponse(
            1L, 101L, 202L, "PASSENGER", 303L, 5, "Awesome ride", Instant.now()
        );
        when(ratingService.submitRating(any(SubmitRatingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.score").value(5))
            .andExpect(jsonPath("$.data.feedback").value("Awesome ride"));
    }

    @Test
    void shouldGetDriverRatingSummary() throws Exception {
        RatingSummaryResponse summary = new RatingSummaryResponse(
            303L, 4.85, 20, Map.of(5, 17L, 4, 3L, 3, 0L, 2, 0L, 1, 0L)
        );
        when(ratingService.getRateeSummary(303L)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/ratings/drivers/303/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.rateeId").value(303))
            .andExpect(jsonPath("$.data.averageRating").value(4.85))
            .andExpect(jsonPath("$.data.totalRatings").value(20));
    }

    @Test
    void shouldGetRatingsForRide() throws Exception {
        List<RatingResponse> ratings = List.of(
            new RatingResponse(1L, 101L, 202L, "PASSENGER", 303L, 5, "Great driver", Instant.now()),
            new RatingResponse(2L, 101L, 303L, "DRIVER", 202L, 5, "Polite passenger", Instant.now())
        );
        when(ratingService.getRatingsByRideId(101L)).thenReturn(ratings);

        mockMvc.perform(get("/api/v1/ratings/rides/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void shouldRejectInvalidRatingRequest() throws Exception {
        SubmitRatingRequest invalidRequest = new SubmitRatingRequest(
            101L, 202L, "PASSENGER", 303L, 10, "Invalid score"
        );

        mockMvc.perform(post("/api/v1/ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
}
