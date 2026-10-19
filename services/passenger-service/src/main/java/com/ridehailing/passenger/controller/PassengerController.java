package com.ridehailing.passenger.controller;

import com.ridehailing.common.dto.ApiResponse;
import com.ridehailing.common.exception.AccessDeniedException;
import com.ridehailing.passenger.dto.CreatePassengerRequest;
import com.ridehailing.passenger.dto.UpdatePassengerRequest;
import com.ridehailing.passenger.model.PassengerProfile;
import com.ridehailing.passenger.security.AuthenticatedUser;
import com.ridehailing.passenger.security.JwtTokenValidator;
import com.ridehailing.passenger.service.PassengerService;
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
@RequestMapping("/api/v1/passengers")
public class PassengerController {

    private final PassengerService passengerService;
    private final JwtTokenValidator jwtTokenValidator;

    public PassengerController(PassengerService passengerService, JwtTokenValidator jwtTokenValidator) {
        this.passengerService = passengerService;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PassengerProfile>> createProfile(
        @RequestHeader(name = "Authorization", required = false) String authHeader,
        @RequestHeader(name = "X-User-Id", required = false) Long xUserId,
        @Valid @RequestBody CreatePassengerRequest request
    ) {
        AuthenticatedUser user = authenticateAndVerifyHeader(authHeader, xUserId);

        // Enforce ownership: cannot create profile for a different user ID unless ADMIN
        if (!user.userId().equals(request.userId()) && !"ROLE_ADMIN".equals(user.role())) {
            throw new AccessDeniedException("Cannot create a passenger profile for a different user ID");
        }

        PassengerProfile profile = passengerService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Passenger profile created successfully", profile));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PassengerProfile>> getProfileById(
        @RequestHeader(name = "Authorization", required = false) String authHeader,
        @RequestHeader(name = "X-User-Id", required = false) Long xUserId,
        @PathVariable("id") Long id
    ) {
        AuthenticatedUser user = authenticateAndVerifyHeader(authHeader, xUserId);
        PassengerProfile profile = passengerService.getById(id);

        // Enforce ownership check: profile must belong to the authenticated caller or ADMIN
        if (!profile.userId().equals(user.userId()) && !"ROLE_ADMIN".equals(user.role())) {
            throw new AccessDeniedException("You are not authorized to view this passenger profile");
        }

        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PassengerProfile>> getMyProfile(
        @RequestHeader(name = "Authorization", required = false) String authHeader,
        @RequestHeader(name = "X-User-Id", required = false) Long xUserId
    ) {
        AuthenticatedUser user = authenticateAndVerifyHeader(authHeader, xUserId);
        PassengerProfile profile = passengerService.getByUserId(user.userId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PassengerProfile>> updateProfile(
        @RequestHeader(name = "Authorization", required = false) String authHeader,
        @RequestHeader(name = "X-User-Id", required = false) Long xUserId,
        @PathVariable("id") Long id,
        @RequestBody UpdatePassengerRequest request
    ) {
        AuthenticatedUser user = authenticateAndVerifyHeader(authHeader, xUserId);
        PassengerProfile existing = passengerService.getById(id);

        // Enforce ownership check: profile must belong to the authenticated caller or ADMIN
        if (!existing.userId().equals(user.userId()) && !"ROLE_ADMIN".equals(user.role())) {
            throw new AccessDeniedException("You are not authorized to modify this passenger profile");
        }

        PassengerProfile updated = passengerService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("Passenger profile updated successfully", updated));
    }

    private AuthenticatedUser authenticateAndVerifyHeader(String authHeader, Long xUserId) {
        AuthenticatedUser user = jwtTokenValidator.validateAndExtract(authHeader);

        // If an explicit X-User-Id header is supplied, it must match the verified token subject
        if (xUserId != null && !xUserId.equals(user.userId())) {
            throw new AccessDeniedException("Provided X-User-Id does not match authenticated token identity");
        }

        return user;
    }
}
