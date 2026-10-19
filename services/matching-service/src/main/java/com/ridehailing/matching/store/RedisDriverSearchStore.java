package com.ridehailing.matching.store;

import com.ridehailing.common.dto.DriverCandidateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RedisDriverSearchStore implements DriverSearchStore {

    private static final Logger log = LoggerFactory.getLogger(RedisDriverSearchStore.class);

    public static final String GEO_KEY_AVAILABLE = "drivers:available";
    public static final String HEARTBEAT_KEY_PREFIX = "driver:heartbeat:";
    public static final String LOCK_KEY_PREFIX = "driver:lock:";

    private final StringRedisTemplate redisTemplate;

    // Resilient in-memory fallback for isolated testing and failover
    private final ConcurrentHashMap<Long, Point> inMemoryGeo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> inMemoryHeartbeatExpiry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> inMemoryLocks = new ConcurrentHashMap<>();

    public RedisDriverSearchStore() {
        this(null);
    }

    @Autowired
    public RedisDriverSearchStore(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<DriverCandidateDto> searchAvailableDrivers(double longitude, double latitude, double radiusKm, int limit) {
        int maxResults = limit > 0 ? limit : 10;

        if (redisTemplate != null) {
            try {
                // Try modern Redis 6.2+ GEOSEARCH command via Spring Data Redis
                GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = redisTemplate.opsForGeo().search(
                    GEO_KEY_AVAILABLE,
                    GeoReference.fromCoordinate(new Point(longitude, latitude)),
                    new Distance(radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS),
                    RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .includeCoordinates()
                        .sortAscending()
                        .limit(maxResults)
                );

                if (geoResults != null && !geoResults.getContent().isEmpty()) {
                    List<DriverCandidateDto> candidates = new ArrayList<>();
                    for (GeoResult<RedisGeoCommands.GeoLocation<String>> res : geoResults) {
                        try {
                            Long driverId = Long.valueOf(res.getContent().getName());
                            double distanceKm = res.getDistance().getValue();
                            Point pt = res.getContent().getPoint();
                            double lat = pt != null ? pt.getY() : latitude;
                            double lon = pt != null ? pt.getX() : longitude;

                            // Stale eviction check: companion heartbeat must exist
                            if (!hasActiveHeartbeat(driverId)) {
                                log.info("Evicting stale driver {} without active heartbeat from {}", driverId, GEO_KEY_AVAILABLE);
                                evictStaleDriver(driverId);
                                continue;
                            }

                            candidates.add(new DriverCandidateDto(driverId, distanceKm, lat, lon, 5.0));
                        } catch (NumberFormatException nfe) {
                            log.warn("Invalid driver member format in geo set: {}", res.getContent().getName());
                        }
                    }
                    if (!candidates.isEmpty() || inMemoryGeo.isEmpty()) {
                        return candidates;
                    }
                }
            } catch (Exception e) {
                log.warn("Redis GEOSEARCH failed, attempting fallback: {}", e.getMessage());
            }
        }

        // In-memory fallback geospatial search using Haversine formula
        List<DriverCandidateDto> inMemoryCandidates = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (var entry : inMemoryGeo.entrySet()) {
            Long driverId = entry.getKey();
            Point pt = entry.getValue();

            // Heartbeat check
            Long expiry = inMemoryHeartbeatExpiry.get(driverId);
            if (expiry == null || now > expiry) {
                evictStaleDriver(driverId);
                continue;
            }

            double dist = haversineDistanceKm(latitude, longitude, pt.getY(), pt.getX());
            if (dist <= radiusKm) {
                inMemoryCandidates.add(new DriverCandidateDto(driverId, dist, pt.getY(), pt.getX(), 5.0));
            }
        }

        inMemoryCandidates.sort((a, b) -> Double.compare(a.distanceKm(), b.distanceKm()));
        if (inMemoryCandidates.size() > maxResults) {
            return inMemoryCandidates.subList(0, maxResults);
        }
        return inMemoryCandidates;
    }

    @Override
    public boolean hasActiveHeartbeat(Long driverId) {
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + driverId;

        if (redisTemplate != null) {
            try {
                Boolean hasKey = redisTemplate.hasKey(heartbeatKey);
                return Boolean.TRUE.equals(hasKey);
            } catch (Exception e) {
                log.warn("Redis hasKey failed checking heartbeat: {}", e.getMessage());
            }
        }

        Long expiry = inMemoryHeartbeatExpiry.get(driverId);
        return expiry != null && System.currentTimeMillis() <= expiry;
    }

    @Override
    public void evictStaleDriver(Long driverId) {
        String member = driverId.toString();
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + member;

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForZSet().remove(GEO_KEY_AVAILABLE, member);
                redisTemplate.delete(heartbeatKey);
                return;
            } catch (Exception e) {
                log.warn("Redis evict failed: {}", e.getMessage());
            }
        }

        inMemoryGeo.remove(driverId);
        inMemoryHeartbeatExpiry.remove(driverId);
    }

    @Override
    public boolean acquireDriverLock(Long driverId, Long rideId, long lockDurationSeconds) {
        String lockKey = LOCK_KEY_PREFIX + driverId;
        long durationSec = lockDurationSeconds > 0 ? lockDurationSeconds : 30;

        if (redisTemplate != null) {
            try {
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    rideId.toString(),
                    Duration.ofSeconds(durationSec)
                );
                return Boolean.TRUE.equals(acquired);
            } catch (Exception e) {
                log.warn("Redis acquire lock failed: {}", e.getMessage());
            }
        }

        long now = System.currentTimeMillis();
        Long existingExpiry = inMemoryLocks.get(driverId);
        if (existingExpiry != null && now <= existingExpiry) {
            return false; // Already locked
        }
        inMemoryLocks.put(driverId, now + (durationSec * 1000L));
        return true;
    }

    @Override
    public void releaseDriverLock(Long driverId) {
        String lockKey = LOCK_KEY_PREFIX + driverId;

        if (redisTemplate != null) {
            try {
                redisTemplate.delete(lockKey);
                return;
            } catch (Exception e) {
                log.warn("Redis release lock failed: {}", e.getMessage());
            }
        }

        inMemoryLocks.remove(driverId);
    }

    // Helper for testing seed data in Redis and in-memory
    public void seedDriver(Long driverId, double longitude, double latitude, long ttlSeconds) {
        inMemoryGeo.put(driverId, new Point(longitude, latitude));
        inMemoryHeartbeatExpiry.put(driverId, System.currentTimeMillis() + (ttlSeconds * 1000L));

        if (redisTemplate != null) {
            try {
                String member = driverId.toString();
                redisTemplate.opsForGeo().add(GEO_KEY_AVAILABLE, new Point(longitude, latitude), member);
                if (ttlSeconds > 0) {
                    redisTemplate.opsForValue().set(HEARTBEAT_KEY_PREFIX + member, "AVAILABLE", Duration.ofSeconds(ttlSeconds));
                } else {
                    redisTemplate.delete(HEARTBEAT_KEY_PREFIX + member);
                }
            } catch (Exception e) {
                log.warn("Redis seed failed: {}", e.getMessage());
            }
        }
    }

    public void seedInMemoryDriver(Long driverId, double longitude, double latitude, long ttlSeconds) {
        seedDriver(driverId, longitude, latitude, ttlSeconds);
    }

    public void clear() {
        inMemoryGeo.clear();
        inMemoryHeartbeatExpiry.clear();
        inMemoryLocks.clear();

        if (redisTemplate != null) {
            try {
                redisTemplate.delete(GEO_KEY_AVAILABLE);
            } catch (Exception e) {
                log.warn("Redis clear failed: {}", e.getMessage());
            }
        }
    }

    public void clearInMemory() {
        clear();
    }

    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }
}
