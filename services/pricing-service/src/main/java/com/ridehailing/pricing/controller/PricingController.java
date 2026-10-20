package com.ridehailing.pricing.controller;

import com.ridehailing.common.dto.ApiResponse;
import com.ridehailing.pricing.dto.FareEstimateRequest;
import com.ridehailing.pricing.dto.FareEstimateResponse;
import com.ridehailing.pricing.dto.PricingZoneDto;
import com.ridehailing.pricing.dto.UpdateSurgeMultiplierRequest;
import com.ridehailing.pricing.service.PricingCalculatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing")
public class PricingController {

    private final PricingCalculatorService pricingService;

    public PricingController(PricingCalculatorService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/estimate")
    public ResponseEntity<ApiResponse<FareEstimateResponse>> estimateFare(
        @Valid @RequestBody FareEstimateRequest request
    ) {
        FareEstimateResponse estimate = pricingService.calculateEstimate(request);
        return ResponseEntity.ok(ApiResponse.success(estimate));
    }

    @GetMapping("/surge-zone/{zoneCode}")
    public ResponseEntity<ApiResponse<PricingZoneDto>> getZone(
        @PathVariable String zoneCode
    ) {
        PricingZoneDto zone = pricingService.getZoneByCode(zoneCode);
        return ResponseEntity.ok(ApiResponse.success(zone));
    }

    @GetMapping("/zones")
    public ResponseEntity<ApiResponse<List<PricingZoneDto>>> getAllZones() {
        List<PricingZoneDto> zones = pricingService.getActiveZones();
        return ResponseEntity.ok(ApiResponse.success(zones));
    }

    @PutMapping("/surge-zone/{zoneCode}/multiplier")
    public ResponseEntity<ApiResponse<PricingZoneDto>> updateSurgeMultiplier(
        @PathVariable String zoneCode,
        @Valid @RequestBody UpdateSurgeMultiplierRequest request
    ) {
        PricingZoneDto updated = pricingService.updateSurgeMultiplier(zoneCode, request.surgeMultiplier());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
}
