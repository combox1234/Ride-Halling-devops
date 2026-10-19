package com.ridehailing.notification.repository;

import com.ridehailing.notification.model.NotificationRecord;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class NotificationRepository {

    private final Map<String, NotificationRecord> storage = new ConcurrentHashMap<>();
    private final Map<Long, List<NotificationRecord>> rideIndex = new ConcurrentHashMap<>();

    public NotificationRecord save(NotificationRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        storage.put(record.id(), record);
        rideIndex.computeIfAbsent(record.rideId(), k -> new CopyOnWriteArrayList<>()).add(record);
        return record;
    }

    public Optional<NotificationRecord> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<NotificationRecord> findByRideId(Long rideId) {
        if (rideId == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(rideIndex.getOrDefault(rideId, Collections.emptyList()));
    }

    public List<NotificationRecord> findAll() {
        return List.copyOf(storage.values());
    }

    public void clear() {
        storage.clear();
        rideIndex.clear();
    }
}
