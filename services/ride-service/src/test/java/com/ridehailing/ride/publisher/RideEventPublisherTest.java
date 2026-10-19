package com.ridehailing.ride.publisher;

import com.ridehailing.common.constant.AmqpConstants;
import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.common.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RideEventPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private RideEventPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new RideEventPublisher(rabbitTemplate);
    }

    @Test
    @DisplayName("publishRideRequested serializes event into EventEnvelope with traceId and publishes to topic exchange")
    void shouldPublishRideRequestedEvent() {
        RideRequestedEvent event = new RideRequestedEvent(
            101L,
            202L,
            new LocationDto(37.7749, -122.4194, "Market St"),
            new LocationDto(37.7849, -122.4094, "Mission St"),
            BigDecimal.valueOf(18.50),
            Instant.now()
        );

        publisher.publishRideRequested(event, "trace-abc-123");

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(
            eq(AmqpConstants.RIDE_TOPIC_EXCHANGE),
            eq(AmqpConstants.ROUTING_KEY_RIDE_REQUESTED),
            captor.capture()
        );

        EventEnvelope envelope = captor.getValue();
        assertThat(envelope.eventId()).isNotNull();
        assertThat(envelope.eventType()).isEqualTo("RideRequestedEvent");
        assertThat(envelope.timestamp()).isNotNull();
        assertThat(envelope.traceId()).isEqualTo("trace-abc-123");
        assertThat(envelope.payload()).isEqualTo(event);
    }

    @Test
    @DisplayName("publishRideStarted publishes to ride.started routing key")
    void shouldPublishRideStartedEvent() {
        RideStartedEvent event = new RideStartedEvent(101L, 303L, 202L, Instant.now());

        publisher.publishRideStarted(event, "trace-start-1");

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(
            eq(AmqpConstants.RIDE_TOPIC_EXCHANGE),
            eq(AmqpConstants.ROUTING_KEY_RIDE_STARTED),
            captor.capture()
        );

        EventEnvelope envelope = captor.getValue();
        assertThat(envelope.eventType()).isEqualTo("RideStartedEvent");
        assertThat(envelope.payload()).isEqualTo(event);
    }

    @Test
    @DisplayName("publishRideCompleted publishes to ride.completed routing key")
    void shouldPublishRideCompletedEvent() {
        RideCompletedEvent event = new RideCompletedEvent(
            101L,
            202L,
            303L,
            BigDecimal.valueOf(22.00),
            "PAY-RIDE-101",
            Instant.now()
        );

        publisher.publishRideCompleted(event, null);

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(
            eq(AmqpConstants.RIDE_TOPIC_EXCHANGE),
            eq(AmqpConstants.ROUTING_KEY_RIDE_COMPLETED),
            captor.capture()
        );

        EventEnvelope envelope = captor.getValue();
        assertThat(envelope.eventType()).isEqualTo("RideCompletedEvent");
        assertThat(envelope.traceId()).isNotNull();
        assertThat(envelope.payload()).isEqualTo(event);
    }

    @Test
    @DisplayName("publishRideCancelled publishes to ride.cancelled routing key")
    void shouldPublishRideCancelledEvent() {
        RideCancelledEvent event = new RideCancelledEvent(
            101L,
            202L,
            null,
            "PASSENGER_CANCELLED",
            Instant.now()
        );

        publisher.publishRideCancelled(event, "trace-cancel-1");

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(
            eq(AmqpConstants.RIDE_TOPIC_EXCHANGE),
            eq(AmqpConstants.ROUTING_KEY_RIDE_CANCELLED),
            captor.capture()
        );

        EventEnvelope envelope = captor.getValue();
        assertThat(envelope.eventType()).isEqualTo("RideCancelledEvent");
        assertThat(envelope.payload()).isEqualTo(event);
    }

    @Test
    @DisplayName("Handles null RabbitTemplate gracefully without throwing exceptions")
    void shouldHandleNullRabbitTemplateGracefully() {
        RideEventPublisher nullPublisher = new RideEventPublisher(null);
        RideRequestedEvent event = new RideRequestedEvent(
            1L, 2L,
            new LocationDto(10.0, 10.0, "Addr1"),
            new LocationDto(11.0, 11.0, "Addr2"),
            BigDecimal.TEN,
            Instant.now()
        );

        // Should not throw NPE
        nullPublisher.publishRideRequested(event, "trace-null");
    }
}
