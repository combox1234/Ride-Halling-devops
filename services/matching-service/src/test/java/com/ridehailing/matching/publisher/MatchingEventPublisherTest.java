package com.ridehailing.matching.publisher;

import com.ridehailing.common.constant.AmqpConstants;
import com.ridehailing.common.event.EventEnvelope;
import com.ridehailing.common.event.RideMatchedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MatchingEventPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private MatchingEventPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new MatchingEventPublisher(rabbitTemplate);
    }

    @Test
    @DisplayName("publishRideMatched wraps event in EventEnvelope and sends to ride.topic.exchange with ride.matched")
    void shouldPublishRideMatchedEvent() {
        RideMatchedEvent event = new RideMatchedEvent(501L, 999L, 404L, Instant.now());

        publisher.publishRideMatched(event, "trace-match-xyz");

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(
            eq(AmqpConstants.RIDE_TOPIC_EXCHANGE),
            eq(AmqpConstants.ROUTING_KEY_RIDE_MATCHED),
            captor.capture()
        );

        EventEnvelope envelope = captor.getValue();
        assertThat(envelope.eventId()).isNotNull();
        assertThat(envelope.eventType()).isEqualTo("RideMatchedEvent");
        assertThat(envelope.traceId()).isEqualTo("trace-match-xyz");
        assertThat(envelope.payload()).isEqualTo(event);
    }

    @Test
    @DisplayName("Handles null RabbitTemplate gracefully")
    void shouldHandleNullRabbitTemplateGracefully() {
        MatchingEventPublisher nullPublisher = new MatchingEventPublisher(null);
        RideMatchedEvent event = new RideMatchedEvent(501L, 999L, 404L, Instant.now());

        nullPublisher.publishRideMatched(event, "trace-test");
    }
}
