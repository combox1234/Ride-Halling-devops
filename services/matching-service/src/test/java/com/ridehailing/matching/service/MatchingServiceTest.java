package com.ridehailing.matching.service;

import com.ridehailing.common.dto.DriverCandidateDto;
import com.ridehailing.common.dto.DriverMatchRequest;
import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.matching.store.RedisDriverSearchStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingServiceTest {

    private RedisDriverSearchStore searchStore;
    private MatchingService matchingService;

    private final LocationDto pickup = new LocationDto(37.7749, -122.4194, "Market St, San Francisco");

    @BeforeEach
    void setUp() {
        searchStore = new RedisDriverSearchStore(null);
        searchStore.clearInMemory();
        matchingService = new MatchingService(searchStore);
    }

    @Test
    @DisplayName("Should select closest available driver and acquire reservation lock")
    void shouldSelectClosestDriver() {
        // Driver 201: ~1.5 km away
        searchStore.seedInMemoryDriver(201L, -122.4050, 37.7850, 60);
        // Driver 202: ~0.5 km away
        searchStore.seedInMemoryDriver(202L, -122.4150, 37.7760, 60);

        DriverMatchRequest request = new DriverMatchRequest(501L, pickup, 5.0);
        Optional<DriverCandidateDto> matched = matchingService.findBestDriver(request);

        assertThat(matched).isPresent();
        assertThat(matched.get().driverId()).isEqualTo(202L);

        // Verify driver 202 is now locked against another ride
        boolean canLockAgain = searchStore.acquireDriverLock(202L, 502L, 30);
        assertThat(canLockAgain).isFalse();
    }

    @Test
    @DisplayName("Should skip locked driver and match next available candidate")
    void shouldSkipLockedDriverAndMatchNext() {
        // Driver 202 is closest, but already locked by Ride 999
        searchStore.seedInMemoryDriver(202L, -122.4150, 37.7760, 60);
        searchStore.acquireDriverLock(202L, 999L, 30);

        // Driver 201 is second closest and unlocked
        searchStore.seedInMemoryDriver(201L, -122.4050, 37.7850, 60);

        DriverMatchRequest request = new DriverMatchRequest(501L, pickup, 5.0);
        Optional<DriverCandidateDto> matched = matchingService.findBestDriver(request);

        assertThat(matched).isPresent();
        assertThat(matched.get().driverId()).isEqualTo(201L);
    }

    @Test
    @DisplayName("Should return empty optional when no drivers exist within search radius")
    void shouldReturnEmptyWhenNoDriversInRadius() {
        // Driver is far outside 5km radius
        searchStore.seedInMemoryDriver(301L, -122.1000, 37.5000, 60);

        DriverMatchRequest request = new DriverMatchRequest(501L, pickup, 5.0);
        Optional<DriverCandidateDto> matched = matchingService.findBestDriver(request);

        assertThat(matched).isEmpty();
    }
}
