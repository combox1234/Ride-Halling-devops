package com.ridehailing.ride.client;

import com.ridehailing.common.dto.DriverCandidateDto;
import com.ridehailing.common.dto.LocationDto;

import java.util.Optional;

public interface DriverMatchingClient {

    /**
     * Queries matching-service for an available, non-stale driver within proximity of the pickup location.
     */
    Optional<DriverCandidateDto> findDriverForRide(Long rideId, LocationDto pickupLocation);
}
