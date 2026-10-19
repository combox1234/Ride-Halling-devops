package com.ridehailing.matching.store;

import com.ridehailing.common.dto.DriverCandidateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisDriverSearchStoreTest {

    private RedisDriverSearchStore searchStore;

    @BeforeEach
    void setUp() {
        searchStore = new RedisDriverSearchStore(null);
        searchStore.clearInMemory();
    }

    @Test
    @DisplayName("Should return available drivers ordered ascending by distance")
    void shouldReturnDriversOrderedByProximity() {
        // Pickup at Market St SF (37.7749, -122.4194)
        double pickupLat = 37.7749;
        double pickupLon = -122.4194;

        // Driver 1: ~1.5 km away
        searchStore.seedInMemoryDriver(201L, -122.4050, 37.7850, 60);
        // Driver 2: ~0.5 km away
        searchStore.seedInMemoryDriver(202L, -122.4150, 37.7760, 60);
        // Driver 3: ~15 km away (outside 5km radius)
        searchStore.seedInMemoryDriver(203L, -122.2500, 37.8000, 60);

        List<DriverCandidateDto> results = searchStore.searchAvailableDrivers(pickupLon, pickupLat, 5.0, 10);

        assertThat(results).hasSize(2);
        // Driver 202 is closest, then 201
        assertThat(results.get(0).driverId()).isEqualTo(202L);
        assertThat(results.get(1).driverId()).isEqualTo(201L);
        assertThat(results.get(0).distanceKm()).isLessThan(results.get(1).distanceKm());
    }

    @Test
    @DisplayName("Should evict and exclude stale drivers whose heartbeat has expired")
    void shouldExcludeAndEvictStaleDrivers() {
        double pickupLat = 37.7749;
        double pickupLon = -122.4194;

        // Driver 201: fresh heartbeat (60s TTL)
        searchStore.seedInMemoryDriver(201L, -122.4150, 37.7760, 60);
        // Driver 202: expired heartbeat (-10s TTL, already stale)
        searchStore.seedInMemoryDriver(202L, -122.4160, 37.7750, -10);

        List<DriverCandidateDto> results = searchStore.searchAvailableDrivers(pickupLon, pickupLat, 5.0, 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).driverId()).isEqualTo(201L);
        // Stale driver 202 should have been evicted
        assertThat(searchStore.hasActiveHeartbeat(202L)).isFalse();
    }

    @Test
    @DisplayName("Should prevent concurrent double-booking via driver lock")
    void shouldPreventConcurrentDoubleBooking() {
        searchStore.seedInMemoryDriver(201L, -122.4150, 37.7760, 60);

        // Ride 1001 acquires lock
        boolean lockedFirst = searchStore.acquireDriverLock(201L, 1001L, 30);
        assertThat(lockedFirst).isTrue();

        // Ride 1002 tries to acquire lock on the same driver -> rejected!
        boolean lockedSecond = searchStore.acquireDriverLock(201L, 1002L, 30);
        assertThat(lockedSecond).isFalse();

        // Release lock
        searchStore.releaseDriverLock(201L);
        boolean lockedAfterRelease = searchStore.acquireDriverLock(201L, 1002L, 30);
        assertThat(lockedAfterRelease).isTrue();
    }
}
