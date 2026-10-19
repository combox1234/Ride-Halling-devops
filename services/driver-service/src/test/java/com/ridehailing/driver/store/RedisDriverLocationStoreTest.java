package com.ridehailing.driver.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Point;

import static org.assertj.core.api.Assertions.assertThat;

class RedisDriverLocationStoreTest {

    private RedisDriverLocationStore store;

    @BeforeEach
    void setUp() {
        // Test fallback store without requiring external live Redis
        store = new RedisDriverLocationStore(null);
    }

    @Test
    @DisplayName("Should record driver location and set active heartbeat")
    void shouldUpdateLocationAndHeartbeat() {
        store.updateLocation(101L, -122.4194, 37.7749, 60);

        assertThat(store.hasActiveHeartbeat(101L)).isTrue();
        Point loc = store.getInMemoryLocation(101L);
        assertThat(loc).isNotNull();
        assertThat(loc.getX()).isEqualTo(-122.4194);
        assertThat(loc.getY()).isEqualTo(37.7749);
    }

    @Test
    @DisplayName("Should evict driver and clear active heartbeat")
    void shouldEvictDriver() {
        store.updateLocation(102L, -122.4014, 37.7892, 60);
        assertThat(store.hasActiveHeartbeat(102L)).isTrue();

        store.evictDriver(102L);

        assertThat(store.hasActiveHeartbeat(102L)).isFalse();
        assertThat(store.getInMemoryLocation(102L)).isNull();
    }
}
