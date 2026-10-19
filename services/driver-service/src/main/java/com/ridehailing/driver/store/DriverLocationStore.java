package com.ridehailing.driver.store;

public interface DriverLocationStore {

    /**
     * Records driver GPS coordinates in the Redis geospatial index 'drivers:available'
     * and sets an availability heartbeat key 'driver:heartbeat:<id>' with a TTL.
     */
    void updateLocation(Long driverId, double longitude, double latitude, long ttlSeconds);

    /**
     * Removes driver from 'drivers:available' and deletes their heartbeat key.
     */
    void evictDriver(Long driverId);

    /**
     * Checks if driver has an active, unexpired heartbeat key.
     */
    boolean hasActiveHeartbeat(Long driverId);
}
