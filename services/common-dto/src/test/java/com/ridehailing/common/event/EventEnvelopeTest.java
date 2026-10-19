package com.ridehailing.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventEnvelopeTest {

    @Test
    @DisplayName("Should create EventEnvelope with explicit parameters")
    void shouldCreateEventEnvelopeWithExplicitParameters() {
        Instant now = Instant.now();
        EventEnvelope<String> envelope = new EventEnvelope<>(
            "evt-123",
            "test.event",
            now,
            "trace-999",
            "test-payload"
        );

        assertThat(envelope.eventId()).isEqualTo("evt-123");
        assertThat(envelope.eventType()).isEqualTo("test.event");
        assertThat(envelope.timestamp()).isEqualTo(now);
        assertThat(envelope.traceId()).isEqualTo("trace-999");
        assertThat(envelope.payload()).isEqualTo("test-payload");
    }

    @Test
    @DisplayName("Should create EventEnvelope using convenience factory method")
    void shouldCreateEventEnvelopeUsingFactory() {
        EventEnvelope<String> envelope = EventEnvelope.of("ride.requested", "trace-100", "payload-data");

        assertThat(envelope.eventId()).isNotBlank();
        assertThat(envelope.eventType()).isEqualTo("ride.requested");
        assertThat(envelope.timestamp()).isNotNull();
        assertThat(envelope.traceId()).isEqualTo("trace-100");
        assertThat(envelope.payload()).isEqualTo("payload-data");
    }

    @Test
    @DisplayName("Should reject null fields in EventEnvelope constructor")
    void shouldRejectNullFields() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> new EventEnvelope<>(null, "type", now, "trace", "data"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("eventId");

        assertThatThrownBy(() -> new EventEnvelope<>("id", null, now, "trace", "data"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("eventType");

        assertThatThrownBy(() -> new EventEnvelope<>("id", "type", null, "trace", "data"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("timestamp");

        assertThatThrownBy(() -> new EventEnvelope<>("id", "type", now, null, "data"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("traceId");

        assertThatThrownBy(() -> new EventEnvelope<>("id", "type", now, "trace", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("payload");
    }
}
