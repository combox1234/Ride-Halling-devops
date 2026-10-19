package com.ridehailing.ride.publisher;

import com.ridehailing.common.constant.AmqpConstants;
import com.ridehailing.common.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RideEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RideEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public RideEventPublisher(@Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRideRequested(RideRequestedEvent event, String traceId) {
        publish(AmqpConstants.ROUTING_KEY_RIDE_REQUESTED, "RideRequestedEvent", traceId, event);
    }

    public void publishRideStarted(RideStartedEvent event, String traceId) {
        publish(AmqpConstants.ROUTING_KEY_RIDE_STARTED, "RideStartedEvent", traceId, event);
    }

    public void publishRideCompleted(RideCompletedEvent event, String traceId) {
        publish(AmqpConstants.ROUTING_KEY_RIDE_COMPLETED, "RideCompletedEvent", traceId, event);
    }

    public void publishRideCancelled(RideCancelledEvent event, String traceId) {
        publish(AmqpConstants.ROUTING_KEY_RIDE_CANCELLED, "RideCancelledEvent", traceId, event);
    }

    private <T> void publish(String routingKey, String eventType, String traceId, T payload) {
        String safeTraceId = (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
        EventEnvelope<T> envelope = EventEnvelope.of(eventType, safeTraceId, payload);
        if (rabbitTemplate != null) {
            try {
                log.info("Publishing event {} with traceId {} to exchange {} with key {}",
                    eventType, safeTraceId, AmqpConstants.RIDE_TOPIC_EXCHANGE, routingKey);
                rabbitTemplate.convertAndSend(AmqpConstants.RIDE_TOPIC_EXCHANGE, routingKey, envelope);
            } catch (org.springframework.amqp.AmqpException e) {
                log.warn("Failed to publish event {} to RabbitMQ (broker unavailable): {}", eventType, e.getMessage());
            }
        } else {
            log.warn("RabbitTemplate is not available. Skipping publishing for event {}", eventType);
        }
    }
}
