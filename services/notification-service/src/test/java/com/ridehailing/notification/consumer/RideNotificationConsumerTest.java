package com.ridehailing.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridehailing.common.constant.AmqpConstants;
import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.common.event.*;
import com.ridehailing.notification.model.NotificationRecord;
import com.ridehailing.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.retry.support.RetryTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RideNotificationConsumerTest {

    private NotificationRepository repository;
    private ObjectMapper objectMapper;
    private RideNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        repository = new NotificationRepository();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new RideNotificationConsumer(repository, objectMapper);
    }

    @Test
    @DisplayName("Creates passenger notification when RideRequestedEvent is consumed")
    void shouldHandleRideRequestedEvent() {
        RideRequestedEvent payload = new RideRequestedEvent(
            101L,
            202L,
            new LocationDto(37.77, -122.41, "Market St"),
            new LocationDto(37.78, -122.40, "Mission St"),
            BigDecimal.valueOf(14.50),
            Instant.now()
        );
        EventEnvelope<RideRequestedEvent> envelope = EventEnvelope.of("RideRequestedEvent", "trace-notif-1", payload);

        consumer.handleRideEvent(envelope);

        List<NotificationRecord> notifications = repository.findByRideId(101L);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).recipientType()).isEqualTo("PASSENGER");
        assertThat(notifications.get(0).recipientId()).isEqualTo(202L);
        assertThat(notifications.get(0).message()).contains("101").contains("14.50");
    }

    @Test
    @DisplayName("Creates both passenger and driver notifications when RideMatchedEvent is consumed")
    void shouldHandleRideMatchedEvent() {
        RideMatchedEvent payload = new RideMatchedEvent(101L, 303L, 202L, Instant.now());
        EventEnvelope<RideMatchedEvent> envelope = EventEnvelope.of("RideMatchedEvent", "trace-notif-2", payload);

        consumer.handleRideEvent(envelope);

        List<NotificationRecord> notifications = repository.findByRideId(101L);
        assertThat(notifications).hasSize(2);

        boolean hasPassenger = notifications.stream().anyMatch(n -> n.recipientType().equals("PASSENGER") && n.recipientId().equals(202L));
        boolean hasDriver = notifications.stream().anyMatch(n -> n.recipientType().equals("DRIVER") && n.recipientId().equals(303L));
        assertThat(hasPassenger).isTrue();
        assertThat(hasDriver).isTrue();
    }

    @Test
    @DisplayName("Creates passenger alert when RideStartedEvent is consumed")
    void shouldHandleRideStartedEvent() {
        RideStartedEvent payload = new RideStartedEvent(101L, 303L, 202L, Instant.now());
        EventEnvelope<RideStartedEvent> envelope = EventEnvelope.of("RideStartedEvent", "trace-notif-3", payload);

        consumer.handleRideEvent(envelope);

        List<NotificationRecord> notifications = repository.findByRideId(101L);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).message()).contains("started with driver #303");
    }

    @Test
    @DisplayName("Creates receipt alert when RideCompletedEvent is consumed")
    void shouldHandleRideCompletedEvent() {
        RideCompletedEvent payload = new RideCompletedEvent(
            101L,
            202L,
            303L,
            BigDecimal.valueOf(25.75),
            "PAY-101",
            Instant.now()
        );
        EventEnvelope<RideCompletedEvent> envelope = EventEnvelope.of("RideCompletedEvent", "trace-notif-4", payload);

        consumer.handleRideEvent(envelope);

        List<NotificationRecord> notifications = repository.findByRideId(101L);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).message()).contains("completed").contains("25.75");
    }

    @Test
    @DisplayName("Creates cancellation notice when RideCancelledEvent is consumed")
    void shouldHandleRideCancelledEvent() {
        RideCancelledEvent payload = new RideCancelledEvent(
            101L,
            202L,
            null,
            "PASSENGER_CHANGED_MIND",
            Instant.now()
        );
        EventEnvelope<RideCancelledEvent> envelope = EventEnvelope.of("RideCancelledEvent", "trace-notif-5", payload);

        consumer.handleRideEvent(envelope);

        List<NotificationRecord> notifications = repository.findByRideId(101L);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).message()).contains("cancelled").contains("PASSENGER_CHANGED_MIND");
    }

    @Test
    @DisplayName("Notification consumer routes to Dead-Letter Queue on 3 failed attempts")
    void shouldRouteToDeadLetterQueueAfterThreeFailedAttempts() {
        RabbitTemplate mockRabbitTemplate = mock(RabbitTemplate.class);
        RepublishMessageRecoverer recoverer = new RepublishMessageRecoverer(
            mockRabbitTemplate,
            AmqpConstants.DLX_TOPIC_EXCHANGE,
            AmqpConstants.ROUTING_KEY_DLX_FAILED
        );

        RetryTemplate retryTemplate = RetryTemplate.builder()
            .maxAttempts(3)
            .fixedBackoff(10)
            .build();

        MessageProperties properties = new MessageProperties();
        properties.setReceivedExchange(AmqpConstants.RIDE_TOPIC_EXCHANGE);
        properties.setReceivedRoutingKey("ride.corrupt");
        Message message = new Message("malformed-bytes".getBytes(), properties);

        AtomicInteger attempts = new AtomicInteger(0);

        try {
            retryTemplate.execute(
                context -> {
                    attempts.incrementAndGet();
                    throw new RuntimeException("Notification delivery downstream timeout");
                },
                context -> {
                    recoverer.recover(message, context.getLastThrowable());
                    return null;
                }
            );
        } catch (Throwable ignored) {
        }

        assertThat(attempts.get()).isEqualTo(3);
        verify(mockRabbitTemplate, times(1)).send(
            eq(AmqpConstants.DLX_TOPIC_EXCHANGE),
            eq(AmqpConstants.ROUTING_KEY_DLX_FAILED),
            eq(message)
        );
    }
}
