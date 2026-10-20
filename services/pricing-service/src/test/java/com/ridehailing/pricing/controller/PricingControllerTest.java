package com.ridehailing.pricing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.pricing.dto.FareEstimateRequest;
import com.ridehailing.pricing.dto.FareEstimateResponse;
import com.ridehailing.pricing.dto.PricingZoneDto;
import com.ridehailing.pricing.dto.UpdateSurgeMultiplierRequest;
import com.ridehailing.pricing.service.PricingCalculatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PricingController.class)
class PricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PricingCalculatorService pricingService;

    @Test
    void shouldEstimateFareSuccessfully() throws Exception {
        LocationDto pickup = new LocationDto(37.7749, -122.4194, "Pickup");
        LocationDto dropoff = new LocationDto(37.8049, -122.4194, "Dropoff");
        FareEstimateRequest request = new FareEstimateRequest(pickup, dropoff, "DEFAULT", 12.0);

        FareEstimateResponse mockResponse = new FareEstimateResponse(
            "DEFAULT", "Standard City Zone",
            BigDecimal.valueOf(5.00), 4.5, BigDecimal.valueOf(2.50), BigDecimal.valueOf(11.25),
            12.0, BigDecimal.valueOf(0.35), BigDecimal.valueOf(4.20),
            BigDecimal.valueOf(1.00), false, BigDecimal.valueOf(20.45), "USD"
        );
        when(pricingService.calculateEstimate(any(FareEstimateRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/pricing/estimate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonExpectation -> {
                assert jsonExpectation.getResponse().getContentAsString().contains("DEFAULT");
                assert jsonExpectation.getResponse().getContentAsString().contains("20.45");
            });
    }

    @Test
    void shouldReturnZoneByCode() throws Exception {
        PricingZoneDto zoneDto = new PricingZoneDto(
            1L, "AIRPORT", "Airport Zone",
            BigDecimal.valueOf(10.00), BigDecimal.valueOf(3.50), BigDecimal.valueOf(0.60),
            BigDecimal.valueOf(1.50), BigDecimal.valueOf(15.00), true
        );
        when(pricingService.getZoneByCode("AIRPORT")).thenReturn(zoneDto);

        mockMvc.perform(get("/api/v1/pricing/surge-zone/AIRPORT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.zoneCode").value("AIRPORT"))
            .andExpect(jsonPath("$.data.surgeMultiplier").value(1.50));
    }

    @Test
    void shouldReturn404WhenZoneNotFound() throws Exception {
        when(pricingService.getZoneByCode("NON_EXISTENT"))
            .thenThrow(new NoSuchElementException("Pricing zone not found with code: NON_EXISTENT"));

        mockMvc.perform(get("/api/v1/pricing/surge-zone/NON_EXISTENT"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAllActiveZones() throws Exception {
        List<PricingZoneDto> zones = List.of(
            new PricingZoneDto(1L, "DEFAULT", "Default", BigDecimal.valueOf(5.0), BigDecimal.valueOf(2.5), BigDecimal.valueOf(0.35), BigDecimal.valueOf(1.0), BigDecimal.valueOf(5.0), true),
            new PricingZoneDto(2L, "DOWNTOWN", "Downtown", BigDecimal.valueOf(6.0), BigDecimal.valueOf(3.0), BigDecimal.valueOf(0.50), BigDecimal.valueOf(1.2), BigDecimal.valueOf(6.0), true)
        );
        when(pricingService.getActiveZones()).thenReturn(zones);

        mockMvc.perform(get("/api/v1/pricing/zones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void shouldUpdateSurgeMultiplier() throws Exception {
        PricingZoneDto updatedZone = new PricingZoneDto(
            1L, "DOWNTOWN", "Downtown",
            BigDecimal.valueOf(6.0), BigDecimal.valueOf(3.0), BigDecimal.valueOf(0.50),
            BigDecimal.valueOf(2.00), BigDecimal.valueOf(6.0), true
        );
        when(pricingService.updateSurgeMultiplier(eq("DOWNTOWN"), any(BigDecimal.class))).thenReturn(updatedZone);

        UpdateSurgeMultiplierRequest request = new UpdateSurgeMultiplierRequest(BigDecimal.valueOf(2.00));

        mockMvc.perform(put("/api/v1/pricing/surge-zone/DOWNTOWN/multiplier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.surgeMultiplier").value(2.00));
    }

    @Test
    void shouldRejectInvalidSurgeMultiplierRequest() throws Exception {
        UpdateSurgeMultiplierRequest invalidRequest = new UpdateSurgeMultiplierRequest(BigDecimal.valueOf(0.50));

        mockMvc.perform(put("/api/v1/pricing/surge-zone/DOWNTOWN/multiplier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
}
