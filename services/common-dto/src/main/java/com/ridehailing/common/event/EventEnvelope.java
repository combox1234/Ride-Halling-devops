package com.ridehailing.common.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EventEnvelope<T>(
    String eventId,
    String eventType,
    Instant timestamp,
    String traceId,
    T payload
) {
    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(traceId, "traceId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    public static <T> EventEnvelope<T> of(String eventType, String traceId, T payload) {
        return new EventEnvelope<>(
            UUID.randomUUID().toString(),
            eventType,
            Instant.now(),
            traceId,
            payload
        );
    }
}
