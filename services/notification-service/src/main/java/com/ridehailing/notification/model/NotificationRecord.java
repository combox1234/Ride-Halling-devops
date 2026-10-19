package com.ridehailing.notification.model;

import java.time.Instant;
import java.util.Objects;

public record NotificationRecord(
    String id,
    Long rideId,
    Long recipientId,
    String recipientType,
    String eventType,
    String message,
    Instant timestamp,
    String status
) {
    public NotificationRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(rideId, "rideId must not be null");
        Objects.requireNonNull(recipientType, "recipientType must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
