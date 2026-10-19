package com.ridehailing.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.constant.AmqpConstants;
import com.ridehailing.common.event.*;
import com.ridehailing.notification.model.NotificationRecord;
import com.ridehailing.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class RideNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(RideNotificationConsumer.class);

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public RideNotificationConsumer(NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = AmqpConstants.QUEUE_RIDE_NOTIFICATION)
    public void handleRideEvent(EventEnvelope<?> envelope) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        log.info("Notification service received event {} [id={}] with traceId {}",
            envelope.eventType(), envelope.eventId(), envelope.traceId());

        processEnvelope(envelope);
    }

    public void processEnvelope(EventEnvelope<?> envelope) {
        String eventType = envelope.eventType();
        Object payload = envelope.payload();

        switch (eventType) {
            case "RideRequestedEvent" -> {
                RideRequestedEvent event = convertPayload(payload, RideRequestedEvent.class);
                saveNotification(
                    event.rideId(),
                    event.passengerId(),
                    "PASSENGER",
                    eventType,
                    "Your ride request #" + event.rideId() + " has been received. Estimated fare: $" + event.estimatedFare().setScale(2, java.math.RoundingMode.HALF_UP)
                );
            }
            case "RideMatchedEvent" -> {
                RideMatchedEvent event = convertPayload(payload, RideMatchedEvent.class);
                saveNotification(
                    event.rideId(),
                    event.passengerId(),
                    "PASSENGER",
                    eventType,
                    "Driver #" + event.driverId() + " has been assigned to your ride #" + event.rideId()
                );
                saveNotification(
                    event.rideId(),
                    event.driverId(),
                    "DRIVER",
                    eventType,
                    "You have been matched with ride #" + event.rideId() + " for passenger #" + event.passengerId()
                );
            }
            case "RideStartedEvent" -> {
                RideStartedEvent event = convertPayload(payload, RideStartedEvent.class);
                saveNotification(
                    event.rideId(),
                    event.passengerId(),
                    "PASSENGER",
                    eventType,
                    "Your ride #" + event.rideId() + " has started with driver #" + event.driverId()
                );
            }
            case "RideCompletedEvent" -> {
                RideCompletedEvent event = convertPayload(payload, RideCompletedEvent.class);
                saveNotification(
                    event.rideId(),
                    event.passengerId(),
                    "PASSENGER",
                    eventType,
                    "Ride #" + event.rideId() + " completed! Final fare: $" + event.finalFare().setScale(2, java.math.RoundingMode.HALF_UP)
                );
            }
            case "RideCancelledEvent" -> {
                RideCancelledEvent event = convertPayload(payload, RideCancelledEvent.class);
                saveNotification(
                    event.rideId(),
                    event.passengerId(),
                    "PASSENGER",
                    eventType,
                    "Ride #" + event.rideId() + " was cancelled. Reason: " + event.reason()
                );
            }
            default -> log.warn("Unhandled event type in notification consumer: {}", eventType);
        }
    }

    private <T> T convertPayload(Object payload, Class<T> clazz) {
        if (clazz.isInstance(payload)) {
            return clazz.cast(payload);
        }
        try {
            return objectMapper.convertValue(payload, clazz);
        } catch (Exception e) {
            log.error("Failed to parse event payload to {}: {}", clazz.getSimpleName(), payload, e);
            throw new IllegalArgumentException("Payload cannot be parsed to " + clazz.getSimpleName(), e);
        }
    }

    private void saveNotification(Long rideId, Long recipientId, String recipientType, String eventType, String message) {
        NotificationRecord record = new NotificationRecord(
            UUID.randomUUID().toString(),
            rideId,
            recipientId,
            recipientType,
            eventType,
            message,
            Instant.now(),
            "SENT"
        );
        notificationRepository.save(record);
        log.info("Recorded {} notification for rideId={}, recipientId={}: {}", recipientType, rideId, recipientId, message);
    }
}
