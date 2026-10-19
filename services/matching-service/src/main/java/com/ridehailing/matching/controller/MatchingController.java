package com.ridehailing.matching.controller;

import com.ridehailing.common.dto.ApiResponse;
import com.ridehailing.common.dto.DriverCandidateDto;
import com.ridehailing.common.dto.DriverMatchRequest;
import com.ridehailing.matching.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/matching")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/find-driver")
    public ResponseEntity<ApiResponse<DriverCandidateDto>> findDriver(@Valid @RequestBody DriverMatchRequest request) {
        DriverCandidateDto candidate = matchingService.findBestDriver(request)
            .orElseThrow(() -> new NoSuchElementException("No available driver found within " + request.radiusKm() + " km radius"));

        return ResponseEntity.ok(ApiResponse.success("Driver matched successfully", candidate));
    }

    @GetMapping("/nearby-drivers")
    public ResponseEntity<ApiResponse<List<DriverCandidateDto>>> getNearbyDrivers(
        @RequestParam("latitude") Double latitude,
        @RequestParam("longitude") Double longitude,
        @RequestParam(value = "radiusKm", defaultValue = "5.0") Double radiusKm
    ) {
        List<DriverCandidateDto> nearby = matchingService.getNearbyDrivers(latitude, longitude, radiusKm);
        return ResponseEntity.ok(ApiResponse.success(nearby));
    }
}
