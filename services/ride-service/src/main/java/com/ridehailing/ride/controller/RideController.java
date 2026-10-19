package com.ridehailing.ride.controller;

import com.ridehailing.common.dto.ApiResponse;
import com.ridehailing.ride.dto.RideBookingRequest;
import com.ridehailing.ride.dto.RideResponse;
import com.ridehailing.ride.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<RideResponse>> requestRide(@Valid @RequestBody RideBookingRequest request) {
        RideResponse response = rideService.requestRide(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Ride requested and driver assigned successfully", response));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<RideResponse>> startRide(@PathVariable("id") Long id) {
        RideResponse response = rideService.startRide(id);
        return ResponseEntity.ok(ApiResponse.success("Ride started successfully", response));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<RideResponse>> completeRide(@PathVariable("id") Long id) {
        RideResponse response = rideService.completeRide(id);
        return ResponseEntity.ok(ApiResponse.success("Ride completed successfully", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RideResponse>> cancelRide(@PathVariable("id") Long id) {
        RideResponse response = rideService.cancelRide(id);
        return ResponseEntity.ok(ApiResponse.success("Ride cancelled successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RideResponse>> getRide(@PathVariable("id") Long id) {
        RideResponse response = rideService.getRide(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
