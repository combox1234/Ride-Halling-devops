package com.ridehailing.matching.store;

import com.ridehailing.common.dto.DriverCandidateDto;

import java.util.List;

public interface DriverSearchStore {

    /**
     * Executes Redis GEOSEARCH against 'drivers:available' within radiusKm of (longitude, latitude).
     */
    List<DriverCandidateDto> searchAvailableDrivers(double longitude, double latitude, double radiusKm, int limit);

    /**
     * Verifies companion heartbeat key 'driver:heartbeat:<driver_id>' exists and has not expired.
     */
    boolean hasActiveHeartbeat(Long driverId);

    /**
     * Evicts stale driver whose heartbeat has expired from 'drivers:available'.
     */
    void evictStaleDriver(Long driverId);

    /**
     * Atomically acquires a lock on the driver for a specific ride to prevent duplicate dispatch under concurrency.
     */
    boolean acquireDriverLock(Long driverId, Long rideId, long lockDurationSeconds);

    /**
     * Releases driver lock.
     */
    void releaseDriverLock(Long driverId);
}
