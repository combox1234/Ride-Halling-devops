package com.ridehailing.driver.controller;

import com.ridehailing.common.dto.ApiResponse;
import com.ridehailing.common.dto.DriverLocationPing;
import com.ridehailing.common.dto.DriverProfileDto;
import com.ridehailing.driver.dto.CreateDriverRequest;
import com.ridehailing.driver.dto.UpdateDriverStatusRequest;
import com.ridehailing.driver.security.AuthenticatedUser;
import com.ridehailing.driver.security.JwtTokenValidator;
import com.ridehailing.driver.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService driverService;
    private final JwtTokenValidator jwtTokenValidator;

    public DriverController(DriverService driverService, JwtTokenValidator jwtTokenValidator) {
        this.driverService = driverService;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DriverProfileDto>> registerDriver(
        @Valid @RequestBody CreateDriverRequest request,
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        AuthenticatedUser caller = jwtTokenValidator.validateAndExtract(authHeader);
        DriverProfileDto profile = driverService.registerDriver(request, caller);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Driver profile registered successfully", profile));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverProfileDto>> getDriver(
        @PathVariable("id") Long id,
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        AuthenticatedUser caller = jwtTokenValidator.validateAndExtract(authHeader);
        DriverProfileDto profile = driverService.getDriver(id, caller);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<DriverProfileDto>> getDriverByUserId(
        @PathVariable("userId") Long userId,
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        AuthenticatedUser caller = jwtTokenValidator.validateAndExtract(authHeader);
        DriverProfileDto profile = driverService.getDriverByUserId(userId, caller);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DriverProfileDto>> updateStatus(
        @PathVariable("id") Long id,
        @Valid @RequestBody UpdateDriverStatusRequest request,
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        AuthenticatedUser caller = jwtTokenValidator.validateAndExtract(authHeader);
        DriverProfileDto profile = driverService.updateStatus(id, request.status(), caller);
        return ResponseEntity.ok(ApiResponse.success("Driver status updated successfully", profile));
    }

    @PostMapping("/location")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
        @Valid @RequestBody DriverLocationPing ping,
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        AuthenticatedUser caller = jwtTokenValidator.validateAndExtract(authHeader);
        driverService.updateLocation(ping, caller);
        return ResponseEntity.ok(ApiResponse.success("Driver location updated successfully", null));
    }
}
