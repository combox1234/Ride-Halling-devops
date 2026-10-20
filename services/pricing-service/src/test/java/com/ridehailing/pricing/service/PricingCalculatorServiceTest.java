package com.ridehailing.pricing.service;

import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.pricing.dto.FareEstimateRequest;
import com.ridehailing.pricing.dto.FareEstimateResponse;
import com.ridehailing.pricing.dto.PricingZoneDto;
import com.ridehailing.pricing.entity.PricingZoneEntity;
import com.ridehailing.pricing.repository.PricingZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingCalculatorServiceTest {

    @Mock
    private PricingZoneRepository pricingZoneRepository;

    private PricingCalculatorService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingCalculatorService(pricingZoneRepository);
    }

    @Test
    void shouldCalculateFareForStandardZoneWithoutSurge() {
        PricingZoneEntity defaultZone = new PricingZoneEntity(
            1L, "DEFAULT", "Standard City Zone",
            BigDecimal.valueOf(5.00), BigDecimal.valueOf(2.50), BigDecimal.valueOf(0.35),
            BigDecimal.valueOf(1.00), BigDecimal.valueOf(5.00), true
        );
        when(pricingZoneRepository.findByZoneCode("DEFAULT")).thenReturn(Optional.of(defaultZone));

        // Pickup and dropoff ~5.55 km apart
        LocationDto pickup = new LocationDto(37.7749, -122.4194, "Market St");
        LocationDto dropoff = new LocationDto(37.8049, -122.4194, "Fisherman's Wharf");
        FareEstimateRequest request = new FareEstimateRequest(pickup, dropoff, "DEFAULT", 15.0);

        FareEstimateResponse response = pricingService.calculateEstimate(request);

        assertNotNull(response);
        assertEquals("DEFAULT", response.zoneCode());
        assertEquals(BigDecimal.valueOf(5.00), response.baseFare());
        assertTrue(response.distanceKm() > 3.0);
        assertTrue(response.estimatedFare().compareTo(BigDecimal.valueOf(10.00)) > 0);
        assertFalse(response.minimumFareApplied());
    }

    @Test
    void shouldApplySurgeMultiplierCorrectly() {
        PricingZoneEntity surgeZone = new PricingZoneEntity(
            2L, "DOWNTOWN", "Downtown Core",
            BigDecimal.valueOf(6.00), BigDecimal.valueOf(3.00), BigDecimal.valueOf(0.50),
            BigDecimal.valueOf(1.50), BigDecimal.valueOf(7.00), true
        );
        when(pricingZoneRepository.findByZoneCode("DOWNTOWN")).thenReturn(Optional.of(surgeZone));

        LocationDto pickup = new LocationDto(37.7749, -122.4194, "Union Square");
        LocationDto dropoff = new LocationDto(37.7849, -122.4194, "Financial District");
        FareEstimateRequest request = new FareEstimateRequest(pickup, dropoff, "DOWNTOWN", 10.0);

        FareEstimateResponse response = pricingService.calculateEstimate(request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(1.50), response.surgeMultiplier());
        // base (6.00) + dist (~1.11 * 3.00 = 3.33) + duration (10 * 0.50 = 5.00) = 14.33 * 1.5 = 21.50
        assertTrue(response.estimatedFare().compareTo(BigDecimal.valueOf(18.00)) > 0);
    }

    @Test
    void shouldApplyMinimumFareGuarantee() {
        PricingZoneEntity airportZone = new PricingZoneEntity(
            3L, "AIRPORT", "Airport Zone",
            BigDecimal.valueOf(10.00), BigDecimal.valueOf(3.50), BigDecimal.valueOf(0.60),
            BigDecimal.valueOf(1.00), BigDecimal.valueOf(25.00), true
        );
        when(pricingZoneRepository.findByZoneCode("AIRPORT")).thenReturn(Optional.of(airportZone));

        // Very short ride in airport zone
        LocationDto pickup = new LocationDto(37.6213, -122.3790, "Terminal 1");
        LocationDto dropoff = new LocationDto(37.6220, -122.3795, "Terminal 2");
        FareEstimateRequest request = new FareEstimateRequest(pickup, dropoff, "AIRPORT", 3.0);

        FareEstimateResponse response = pricingService.calculateEstimate(request);

        assertNotNull(response);
        assertTrue(response.minimumFareApplied());
        assertEquals(BigDecimal.valueOf(25.00), response.estimatedFare());
    }

    @Test
    void shouldThrowExceptionWhenZoneNotFound() {
        when(pricingZoneRepository.findByZoneCode("UNKNOWN")).thenReturn(Optional.empty());

        LocationDto pickup = new LocationDto(37.7749, -122.4194, "A");
        LocationDto dropoff = new LocationDto(37.8049, -122.4194, "B");
        FareEstimateRequest request = new FareEstimateRequest(pickup, dropoff, "UNKNOWN", 10.0);

        assertThrows(NoSuchElementException.class, () -> pricingService.calculateEstimate(request));
    }

    @Test
    void shouldUpdateSurgeMultiplier() {
        PricingZoneEntity entity = new PricingZoneEntity(
            1L, "DEFAULT", "Standard",
            BigDecimal.valueOf(5.00), BigDecimal.valueOf(2.50), BigDecimal.valueOf(0.35),
            BigDecimal.valueOf(1.00), BigDecimal.valueOf(5.00), true
        );
        when(pricingZoneRepository.findByZoneCode("DEFAULT")).thenReturn(Optional.of(entity));
        when(pricingZoneRepository.save(any(PricingZoneEntity.class))).thenAnswer(i -> i.getArgument(0));

        PricingZoneDto updated = pricingService.updateSurgeMultiplier("DEFAULT", BigDecimal.valueOf(2.25));

        assertNotNull(updated);
        assertEquals(BigDecimal.valueOf(2.25), updated.surgeMultiplier());
    }

    @Test
    void shouldRejectInvalidSurgeMultiplier() {
        assertThrows(IllegalArgumentException.class, () ->
            pricingService.updateSurgeMultiplier("DEFAULT", BigDecimal.valueOf(0.50))
        );
        assertThrows(IllegalArgumentException.class, () ->
            pricingService.updateSurgeMultiplier("DEFAULT", BigDecimal.valueOf(6.00))
        );
    }
}
