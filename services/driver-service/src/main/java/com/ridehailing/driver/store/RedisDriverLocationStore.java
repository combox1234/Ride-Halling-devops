package com.ridehailing.driver.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RedisDriverLocationStore implements DriverLocationStore {

    private static final Logger log = LoggerFactory.getLogger(RedisDriverLocationStore.class);

    public static final String GEO_KEY_AVAILABLE = "drivers:available";
    public static final String HEARTBEAT_KEY_PREFIX = "driver:heartbeat:";

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<Long, Point> inMemoryGeo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> inMemoryHeartbeatExpiry = new ConcurrentHashMap<>();

    public RedisDriverLocationStore() {
        this(null);
    }

    @Autowired
    public RedisDriverLocationStore(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void updateLocation(Long driverId, double longitude, double latitude, long ttlSeconds) {
        String member = driverId.toString();
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + member;
        long ttlSec = ttlSeconds > 0 ? ttlSeconds : 60;
        Duration ttl = Duration.ofSeconds(ttlSec);

        if (redisTemplate != null) {
            try {
                // 1. GEOADD drivers:available <lng> <lat> <driver_id>
                redisTemplate.opsForGeo().add(GEO_KEY_AVAILABLE, new Point(longitude, latitude), member);
                // 2. SETEX driver:heartbeat:<id> 60 "AVAILABLE"
                redisTemplate.opsForValue().set(heartbeatKey, "AVAILABLE", ttl);
                return;
            } catch (Exception e) {
                log.warn("Redis operation failed during location update, falling back to local memory: {}", e.getMessage());
            }
        }

        inMemoryGeo.put(driverId, new Point(longitude, latitude));
        inMemoryHeartbeatExpiry.put(driverId, System.currentTimeMillis() + (ttlSec * 1000L));
    }

    @Override
    public void evictDriver(Long driverId) {
        String member = driverId.toString();
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + member;

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForZSet().remove(GEO_KEY_AVAILABLE, member);
                redisTemplate.delete(heartbeatKey);
                return;
            } catch (Exception e) {
                log.warn("Redis operation failed during driver eviction, falling back to local memory: {}", e.getMessage());
            }
        }

        inMemoryGeo.remove(driverId);
        inMemoryHeartbeatExpiry.remove(driverId);
    }

    @Override
    public boolean hasActiveHeartbeat(Long driverId) {
        String member = driverId.toString();
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + member;

        if (redisTemplate != null) {
            try {
                Boolean hasKey = redisTemplate.hasKey(heartbeatKey);
                return Boolean.TRUE.equals(hasKey);
            } catch (Exception e) {
                log.warn("Redis operation failed checking heartbeat, falling back to local memory: {}", e.getMessage());
            }
        }

        Long expiry = inMemoryHeartbeatExpiry.get(driverId);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            inMemoryGeo.remove(driverId);
            inMemoryHeartbeatExpiry.remove(driverId);
            return false;
        }
        return true;
    }

    public Point getInMemoryLocation(Long driverId) {
        return inMemoryGeo.get(driverId);
    }
}
