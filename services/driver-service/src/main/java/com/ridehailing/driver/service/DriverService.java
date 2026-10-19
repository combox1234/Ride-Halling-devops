package com.ridehailing.driver.service;

import com.ridehailing.common.dto.DriverLocationPing;
import com.ridehailing.common.dto.DriverProfileDto;
import com.ridehailing.common.dto.DriverStatus;
import com.ridehailing.common.exception.AccessDeniedException;
import com.ridehailing.driver.dto.CreateDriverRequest;
import com.ridehailing.driver.model.DriverProfile;
import com.ridehailing.driver.repository.DriverRepository;
import com.ridehailing.driver.security.AuthenticatedUser;
import com.ridehailing.driver.store.DriverLocationStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverLocationStore locationStore;

    public DriverService(DriverRepository driverRepository, DriverLocationStore locationStore) {
        this.driverRepository = driverRepository;
        this.locationStore = locationStore;
    }

    @Transactional
    public DriverProfileDto registerDriver(CreateDriverRequest request, AuthenticatedUser caller) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(caller, "caller must not be null");

        // Role & ownership check
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.role());
        if (!isAdmin && !caller.userId().equals(request.userId())) {
            throw new AccessDeniedException("You can only register a driver profile for your own authenticated user account");
        }

        if (driverRepository.existsByUserId(request.userId())) {
            throw new IllegalArgumentException("Driver profile already exists for user ID: " + request.userId());
        }
        if (driverRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Driver email already registered: " + request.email());
        }
        if (driverRepository.existsByLicensePlate(request.licensePlate())) {
            throw new IllegalArgumentException("Vehicle license plate already registered: " + request.licensePlate());
        }

        DriverProfile profile = new DriverProfile(
            null,
            request.userId(),
            request.name().trim(),
            request.email().toLowerCase().trim(),
            request.phone().trim(),
            request.vehicleMake().trim(),
            request.vehicleModel().trim(),
            request.licensePlate().toUpperCase().trim(),
            DriverStatus.OFFLINE,
            5.0,
            0,
            Instant.now()
        );

        DriverProfile saved = driverRepository.save(profile);
        return saved.toDto();
    }

    public DriverProfileDto getDriver(Long id, AuthenticatedUser caller) {
        DriverProfile driver = findDriverOrThrow(id);
        verifyOwnership(driver, caller);
        return driver.toDto();
    }

    public DriverProfileDto getDriverByUserId(Long userId, AuthenticatedUser caller) {
        DriverProfile driver = driverRepository.findByUserId(userId)
            .orElseThrow(() -> new NoSuchElementException("Driver profile not found for user ID: " + userId));
        verifyOwnership(driver, caller);
        return driver.toDto();
    }

    @Transactional
    public DriverProfileDto updateStatus(Long id, DriverStatus newStatus, AuthenticatedUser caller) {
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        DriverProfile driver = findDriverOrThrow(id);
        verifyOwnership(driver, caller);

        if (driver.status() == newStatus) {
            return driver.toDto();
        }

        // If driver transitions away from AVAILABLE (to OFFLINE or ON_TRIP), evict from Redis available pool
        if (newStatus != DriverStatus.AVAILABLE) {
            locationStore.evictDriver(driver.id());
        }

        DriverProfile updated = driver.withStatus(newStatus);
        DriverProfile saved = driverRepository.save(updated);
        return saved.toDto();
    }

    public void updateLocation(DriverLocationPing ping, AuthenticatedUser caller) {
        Objects.requireNonNull(ping, "ping must not be null");
        DriverProfile driver = findDriverOrThrow(ping.driverId());
        verifyOwnership(driver, caller);

        if (driver.status() == DriverStatus.OFFLINE) {
            throw new IllegalStateException("Cannot update location for driver who is OFFLINE. Please set status to AVAILABLE first.");
        }

        if (driver.status() == DriverStatus.AVAILABLE) {
            locationStore.updateLocation(driver.id(), ping.longitude(), ping.latitude(), 60);
        }
    }

    private DriverProfile findDriverOrThrow(Long id) {
        return driverRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Driver not found with ID: " + id));
    }

    private void verifyOwnership(DriverProfile driver, AuthenticatedUser caller) {
        if (caller == null) {
            throw new AccessDeniedException("Unauthenticated request");
        }
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.role());
        if (!isAdmin && !caller.userId().equals(driver.userId())) {
            throw new AccessDeniedException("Access denied: you cannot access or modify another driver's profile");
        }
    }
}
