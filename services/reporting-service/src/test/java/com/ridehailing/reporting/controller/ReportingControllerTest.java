package com.ridehailing.reporting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.reporting.dto.DailyMetricsResponse;
import com.ridehailing.reporting.dto.DriverPerformanceResponse;
import com.ridehailing.reporting.dto.PlatformOverviewResponse;
import com.ridehailing.reporting.dto.ReportingEventResponse;
import com.ridehailing.reporting.service.ReportingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportingController.class)
class ReportingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportingService reportingService;

    @Test
    void shouldGetDailySummary() throws Exception {
        DailyMetricsResponse response = new DailyMetricsResponse(
            LocalDate.of(2026, 10, 20), 45, 40, 38, 2, BigDecimal.valueOf(1140.00)
        );
        when(reportingService.getDailySummary(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/daily-summary?date=2026-10-20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalRidesRequested").value(45))
            .andExpect(jsonPath("$.data.totalRidesCompleted").value(38))
            .andExpect(jsonPath("$.data.totalRevenue").value(1140.00));
    }

    @Test
    void shouldGetDriverPerformance() throws Exception {
        DriverPerformanceResponse response = new DriverPerformanceResponse(
            303L, LocalDate.of(2026, 10, 20), 12, BigDecimal.valueOf(320.00)
        );
        when(reportingService.getDriverPerformance(eq(303L), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/driver-performance/303?date=2026-10-20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.driverId").value(303))
            .andExpect(jsonPath("$.data.completedTrips").value(12))
            .andExpect(jsonPath("$.data.totalEarnings").value(320.00));
    }

    @Test
    void shouldGetOverview() throws Exception {
        PlatformOverviewResponse overview = new PlatformOverviewResponse(
            500L, 450L, 20L, BigDecimal.valueOf(12500.00)
        );
        when(reportingService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/v1/reports/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalRidesRequested").value(500))
            .andExpect(jsonPath("$.data.totalPlatformRevenue").value(12500.00));
    }

    @Test
    void shouldGetRecentEvents() throws Exception {
        List<ReportingEventResponse> events = List.of(
            new ReportingEventResponse(1L, "EVT-1", "RideRequestedEvent", 100L, "TRC-1", Instant.now(), Instant.now())
        );
        when(reportingService.getRecentEvents()).thenReturn(events);

        mockMvc.perform(get("/api/v1/reports/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].eventType").value("RideRequestedEvent"));
    }
}
