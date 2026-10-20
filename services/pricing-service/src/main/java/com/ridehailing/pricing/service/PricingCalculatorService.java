package com.ridehailing.pricing.service;

import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.pricing.dto.FareEstimateRequest;
import com.ridehailing.pricing.dto.FareEstimateResponse;
import com.ridehailing.pricing.dto.PricingZoneDto;
import com.ridehailing.pricing.entity.PricingZoneEntity;
import com.ridehailing.pricing.repository.PricingZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PricingCalculatorService {

    private final PricingZoneRepository pricingZoneRepository;

    public PricingCalculatorService(PricingZoneRepository pricingZoneRepository) {
        this.pricingZoneRepository = pricingZoneRepository;
    }

    @Transactional(readOnly = true)
    public FareEstimateResponse calculateEstimate(FareEstimateRequest request) {
        String zoneCode = (request.zoneCode() != null && !request.zoneCode().isBlank())
            ? request.zoneCode().toUpperCase()
            : "DEFAULT";

        PricingZoneEntity zone = pricingZoneRepository.findByZoneCode(zoneCode)
            .orElseThrow(() -> new NoSuchElementException("Pricing zone not found with code: " + zoneCode));

        LocationDto pickup = request.pickupLocation();
        LocationDto dropoff = request.dropoffLocation();

        double distanceKm = calculateDistanceKm(pickup, dropoff);
        double durationMinutes = request.estimatedDurationMinutes() != null && request.estimatedDurationMinutes() > 0
            ? BigDecimal.valueOf(request.estimatedDurationMinutes()).setScale(1, RoundingMode.HALF_UP).doubleValue()
            : calculateEstimatedDurationMinutes(distanceKm);

        BigDecimal baseFare = zone.getBaseFare();
        BigDecimal perKmRate = zone.getPerKmRate();
        BigDecimal distanceCharge = BigDecimal.valueOf(distanceKm)
            .multiply(perKmRate)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal perMinuteRate = zone.getPerMinuteRate();
        BigDecimal durationCharge = BigDecimal.valueOf(durationMinutes)
            .multiply(perMinuteRate)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal subtotal = baseFare.add(distanceCharge).add(durationCharge);
        BigDecimal surgeMultiplier = zone.getSurgeMultiplier();
        BigDecimal surgedFare = subtotal.multiply(surgeMultiplier).setScale(2, RoundingMode.HALF_UP);

        boolean minApplied = false;
        BigDecimal finalFare = surgedFare;
        if (surgedFare.compareTo(zone.getMinFare()) < 0) {
            finalFare = zone.getMinFare();
            minApplied = true;
        }

        return new FareEstimateResponse(
            zone.getZoneCode(),
            zone.getZoneName(),
            baseFare,
            distanceKm,
            perKmRate,
            distanceCharge,
            durationMinutes,
            perMinuteRate,
            durationCharge,
            surgeMultiplier,
            minApplied,
            finalFare,
            "USD"
        );
    }

    @Transactional(readOnly = true)
    public PricingZoneDto getZoneByCode(String zoneCode) {
        PricingZoneEntity entity = pricingZoneRepository.findByZoneCode(zoneCode.toUpperCase())
            .orElseThrow(() -> new NoSuchElementException("Pricing zone not found with code: " + zoneCode));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<PricingZoneDto> getActiveZones() {
        return pricingZoneRepository.findByActiveTrue().stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public PricingZoneDto updateSurgeMultiplier(String zoneCode, BigDecimal multiplier) {
        if (multiplier.compareTo(BigDecimal.valueOf(1.00)) < 0 || multiplier.compareTo(BigDecimal.valueOf(5.00)) > 0) {
            throw new IllegalArgumentException("Surge multiplier must be between 1.00 and 5.00");
        }

        PricingZoneEntity entity = pricingZoneRepository.findByZoneCode(zoneCode.toUpperCase())
            .orElseThrow(() -> new NoSuchElementException("Pricing zone not found with code: " + zoneCode));

        entity.setSurgeMultiplier(multiplier.setScale(2, RoundingMode.HALF_UP));
        PricingZoneEntity updated = pricingZoneRepository.save(entity);
        return toDto(updated);
    }

    private double calculateDistanceKm(LocationDto pickup, LocationDto dropoff) {
        double latDiff = (dropoff.latitude() - pickup.latitude()) * 111.0;
        double lngDiff = (dropoff.longitude() - pickup.longitude()) * 111.0 * Math.cos(Math.toRadians(pickup.latitude()));
        double distance = Math.sqrt((latDiff * latDiff) + (lngDiff * lngDiff));
        if (distance < 0.5) {
            distance = 0.5;
        }
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double calculateEstimatedDurationMinutes(double distanceKm) {
        // Average city speed: 30 km/h -> 2 minutes per km, minimum 2 minutes
        double minutes = (distanceKm / 30.0) * 60.0;
        if (minutes < 2.0) {
            minutes = 2.0;
        }
        return BigDecimal.valueOf(minutes).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private PricingZoneDto toDto(PricingZoneEntity entity) {
        return new PricingZoneDto(
            entity.getId(),
            entity.getZoneCode(),
            entity.getZoneName(),
            entity.getBaseFare(),
            entity.getPerKmRate(),
            entity.getPerMinuteRate(),
            entity.getSurgeMultiplier(),
            entity.getMinFare(),
            entity.isActive()
        );
    }
}
