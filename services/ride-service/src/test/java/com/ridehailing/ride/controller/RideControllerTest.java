package com.ridehailing.ride.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.ride.dto.RideBookingRequest;
import com.ridehailing.ride.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RideRepository rideRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    private final LocationDto pickup = new LocationDto(37.7749, -122.4194, "Market St, SF");
    private final LocationDto dropoff = new LocationDto(37.7833, -122.4167, "Union Square, SF");

    @BeforeEach
    void setUp() {
        rideRepository.clear();
    }

    @Test
    @DisplayName("Complete end-to-end ride journey: request -> start -> complete")
    void shouldExecuteCompleteRideJourney() throws Exception {
        // 1. Request ride
        RideBookingRequest booking = new RideBookingRequest(101L, pickup, dropoff);
        String createRes = mockMvc.perform(post("/api/v1/rides/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("DRIVER_ASSIGNED"))
            .andExpect(jsonPath("$.data.driverId").value(201))
            .andExpect(jsonPath("$.data.id").value(notNullValue()))
            .andReturn().getResponse().getContentAsString();

        long rideId = objectMapper.readTree(createRes).path("data").path("id").asLong();

        // 2. Start ride
        mockMvc.perform(post("/api/v1/rides/" + rideId + "/start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // 3. Complete ride
        mockMvc.perform(post("/api/v1/rides/" + rideId + "/complete"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.idempotencyKey").value("PAY-RIDE-" + rideId))
            .andExpect(jsonPath("$.data.completedAt").value(notNullValue()));

        // 4. Idempotency test: Repeated completion returns 200 OK with identical payload
        mockMvc.perform(post("/api/v1/rides/" + rideId + "/complete"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.idempotencyKey").value("PAY-RIDE-" + rideId));

        // 5. Verify ride details
        mockMvc.perform(get("/api/v1/rides/" + rideId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/rides/{id}/cancel should return 400 when ride is IN_PROGRESS")
    void shouldReturnBadRequestWhenCancellingInProgressRide() throws Exception {
        RideBookingRequest booking = new RideBookingRequest(101L, pickup, dropoff);
        String createRes = mockMvc.perform(post("/api/v1/rides/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(booking)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        long rideId = objectMapper.readTree(createRes).path("data").path("id").asLong();

        // Start ride
        mockMvc.perform(post("/api/v1/rides/" + rideId + "/start"))
            .andExpect(status().isOk());

        // Cancel attempt should fail
        mockMvc.perform(post("/api/v1/rides/" + rideId + "/cancel"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid State Transition"));
    }

    @Test
    @DisplayName("POST /api/v1/rides/request should return 400 when coordinates out of bounds")
    void shouldReturnBadRequestWhenCoordinatesInvalid() throws Exception {
        String invalidPayload = """
            {
              "passengerId": 101,
              "pickupLocation": {
                "latitude": 95.0,
                "longitude": -122.4194,
                "address": "Invalid Lat"
              },
              "dropoffLocation": {
                "latitude": 37.7833,
                "longitude": -122.4167,
                "address": "Valid"
              }
            }
            """;

        mockMvc.perform(post("/api/v1/rides/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/rides/{id} should return 404 when ride not found")
    void shouldReturn404WhenRideNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/rides/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
