package com.ridehailing.matching.service;

import com.ridehailing.common.dto.DriverCandidateDto;
import com.ridehailing.common.dto.DriverMatchRequest;
import com.ridehailing.matching.store.DriverSearchStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final DriverSearchStore searchStore;

    public MatchingService(DriverSearchStore searchStore) {
        this.searchStore = searchStore;
    }

    public Optional<DriverCandidateDto> findBestDriver(DriverMatchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(request.pickupLocation(), "pickupLocation must not be null");

        double lon = request.pickupLocation().longitude();
        double lat = request.pickupLocation().latitude();
        double radius = request.radiusKm() != null ? request.radiusKm() : 5.0;

        List<DriverCandidateDto> candidates = searchStore.searchAvailableDrivers(lon, lat, radius, 10);
        log.info("Found {} proximity candidates for ride {} at ({}, {}) within {}km",
            candidates.size(), request.rideId(), lat, lon, radius);

        for (DriverCandidateDto candidate : candidates) {
            // Attempt atomic driver reservation to avoid duplicate assignment under concurrency
            boolean locked = searchStore.acquireDriverLock(candidate.driverId(), request.rideId(), 30);
            if (locked) {
                log.info("Successfully matched and locked driver {} for ride {}", candidate.driverId(), request.rideId());
                return Optional.of(candidate);
            }
            log.info("Driver {} is currently reserved by a concurrent match, trying next candidate", candidate.driverId());
        }

        log.warn("No available, non-stale drivers within {}km could be matched for ride {}", radius, request.rideId());
        return Optional.empty();
    }

    public List<DriverCandidateDto> getNearbyDrivers(double latitude, double longitude, double radiusKm) {
        double radius = radiusKm > 0 ? radiusKm : 5.0;
        return searchStore.searchAvailableDrivers(longitude, latitude, radius, 20);
    }
}
