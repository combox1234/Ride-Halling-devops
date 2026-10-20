package com.ridehailing.reporting.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reporting_events")
public class ReportingEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "aggregate_id")
    private Long aggregateId;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    public ReportingEventEntity() {
    }

    public ReportingEventEntity(
        String eventId,
        String eventType,
        Long aggregateId,
        String traceId,
        String payloadJson,
        Instant occurredAt
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.traceId = traceId;
        this.payloadJson = payloadJson;
        this.occurredAt = occurredAt;
        this.recordedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
