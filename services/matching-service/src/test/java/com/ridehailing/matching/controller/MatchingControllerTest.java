package com.ridehailing.matching.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.dto.DriverMatchRequest;
import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.matching.store.RedisDriverSearchStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisDriverSearchStore searchStore;

    private final LocationDto pickup = new LocationDto(37.7749, -122.4194, "Market St, San Francisco");

    @BeforeEach
    void setUp() {
        searchStore.clearInMemory();
    }

    @Test
    @DisplayName("POST /api/v1/matching/find-driver should return matched driver")
    void shouldFindDriver() throws Exception {
        searchStore.seedInMemoryDriver(201L, -122.4150, 37.7760, 60);

        DriverMatchRequest request = new DriverMatchRequest(501L, pickup, 5.0);

        mockMvc.perform(post("/api/v1/matching/find-driver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.driverId").value(201));
    }

    @Test
    @DisplayName("POST /api/v1/matching/find-driver should return 404 when no drivers found")
    void shouldReturn404WhenNoDrivers() throws Exception {
        DriverMatchRequest request = new DriverMatchRequest(502L, pickup, 5.0);

        mockMvc.perform(post("/api/v1/matching/find-driver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("GET /api/v1/matching/nearby-drivers should return nearby candidate list")
    void shouldReturnNearbyDrivers() throws Exception {
        searchStore.seedInMemoryDriver(201L, -122.4150, 37.7760, 60);
        searchStore.seedInMemoryDriver(202L, -122.4100, 37.7780, 60);

        mockMvc.perform(get("/api/v1/matching/nearby-drivers")
                .param("latitude", "37.7749")
                .param("longitude", "-122.4194")
                .param("radiusKm", "5.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(2)));
    }
}
