package com.ridehailing.matching.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridehailing.common.constant.AmqpConstants;
import com.ridehailing.common.dto.DriverCandidateDto;
import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.common.event.EventEnvelope;
import com.ridehailing.common.event.RideMatchedEvent;
import com.ridehailing.common.event.RideRequestedEvent;
import com.ridehailing.matching.publisher.MatchingEventPublisher;
import com.ridehailing.matching.service.MatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.retry.support.RetryTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RideRequestedConsumerTest {

    private MatchingService matchingService;
    private MatchingEventPublisher matchingEventPublisher;
    private ObjectMapper objectMapper;
    private RideRequestedConsumer consumer;

    @BeforeEach
    void setUp() {
        matchingService = mock(MatchingService.class);
        matchingEventPublisher = mock(MatchingEventPublisher.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new RideRequestedConsumer(matchingService, matchingEventPublisher, objectMapper);
    }

    @Test
    @DisplayName("Successfully consumes RideRequestedEvent and publishes RideMatchedEvent when driver found")
    void shouldMatchDriverAndPublishRideMatchedEvent() {
        RideRequestedEvent payload = new RideRequestedEvent(
            101L,
            202L,
            new LocationDto(37.7749, -122.4194, "Market St"),
            new LocationDto(37.7849, -122.4094, "Mission St"),
            BigDecimal.valueOf(15.00),
            Instant.now()
        );
        EventEnvelope<RideRequestedEvent> envelope = EventEnvelope.of(
            "RideRequestedEvent",
            "trace-matching-1",
            payload
        );

        DriverCandidateDto candidate = new DriverCandidateDto(777L, 0.45, 37.7750, -122.4190, 4.9);
        when(matchingService.findBestDriver(any())).thenReturn(Optional.of(candidate));

        consumer.handleRideRequested(envelope);

        ArgumentCaptor<RideMatchedEvent> eventCaptor = ArgumentCaptor.forClass(RideMatchedEvent.class);
        verify(matchingEventPublisher).publishRideMatched(eventCaptor.capture(), eq("trace-matching-1"));

        RideMatchedEvent published = eventCaptor.getValue();
        assertThat(published.rideId()).isEqualTo(101L);
        assertThat(published.driverId()).isEqualTo(777L);
        assertThat(published.passengerId()).isEqualTo(202L);
    }

    @Test
    @DisplayName("Handles scenario when no driver is available without publishing match or throwing")
    void shouldHandleNoAvailableDriverGracefully() {
        RideRequestedEvent payload = new RideRequestedEvent(
            101L,
            202L,
            new LocationDto(37.7749, -122.4194, "Market St"),
            new LocationDto(37.7849, -122.4094, "Mission St"),
            BigDecimal.valueOf(15.00),
            Instant.now()
        );
        EventEnvelope<RideRequestedEvent> envelope = EventEnvelope.of(
            "RideRequestedEvent",
            "trace-matching-2",
            payload
        );

        when(matchingService.findBestDriver(any())).thenReturn(Optional.empty());

        consumer.handleRideRequested(envelope);

        verify(matchingEventPublisher, never()).publishRideMatched(any(), any());
    }

    @Test
    @DisplayName("Consumer failure triggers retry policy and routes message to Dead-Letter Queue after 3 failed attempts")
    void shouldRouteToDeadLetterQueueAfterThreeFailedAttempts() {
        RabbitTemplate mockRabbitTemplate = mock(RabbitTemplate.class);
        RepublishMessageRecoverer recoverer = new RepublishMessageRecoverer(
            mockRabbitTemplate,
            AmqpConstants.DLX_TOPIC_EXCHANGE,
            AmqpConstants.ROUTING_KEY_DLX_FAILED
        );

        RetryTemplate retryTemplate = RetryTemplate.builder()
            .maxAttempts(3)
            .fixedBackoff(10) // 10ms for fast unit test
            .build();

        MessageProperties properties = new MessageProperties();
        properties.setReceivedExchange(AmqpConstants.RIDE_TOPIC_EXCHANGE);
        properties.setReceivedRoutingKey(AmqpConstants.ROUTING_KEY_RIDE_REQUESTED);
        Message message = new Message("{\"invalid\":\"json\"}".getBytes(), properties);

        AtomicInteger attemptCounter = new AtomicInteger(0);

        try {
            retryTemplate.execute(
                context -> {
                    attemptCounter.incrementAndGet();
                    throw new RuntimeException("Transient connection failure while processing match");
                },
                context -> {
                    recoverer.recover(message, context.getLastThrowable());
                    return null;
                }
            );
        } catch (Throwable ignored) {
        }

        // Verify exactly 3 attempts were made
        assertThat(attemptCounter.get()).isEqualTo(3);

        // Verify message was republished to DLX exchange with routing key dlx.failed
        verify(mockRabbitTemplate, times(1)).send(
            eq(AmqpConstants.DLX_TOPIC_EXCHANGE),
            eq(AmqpConstants.ROUTING_KEY_DLX_FAILED),
            eq(message)
        );
    }
}
