package com.ridehailing.reporting.dto;

import java.time.Instant;

public record ReportingEventResponse(
    Long id,
    String eventId,
    String eventType,
    Long aggregateId,
    String traceId,
    Instant occurredAt,
    Instant recordedAt
) {
}
