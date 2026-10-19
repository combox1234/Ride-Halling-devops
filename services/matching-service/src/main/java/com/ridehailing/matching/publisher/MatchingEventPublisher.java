package com.ridehailing.matching.publisher;

import com.ridehailing.common.constant.AmqpConstants;
import com.ridehailing.common.event.EventEnvelope;
import com.ridehailing.common.event.RideMatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MatchingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MatchingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public MatchingEventPublisher(@Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRideMatched(RideMatchedEvent event, String traceId) {
        String safeTraceId = (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
        EventEnvelope<RideMatchedEvent> envelope = EventEnvelope.of("RideMatchedEvent", safeTraceId, event);

        if (rabbitTemplate != null) {
            try {
                log.info("Publishing RideMatchedEvent for rideId {} and driverId {} with traceId {}",
                    event.rideId(), event.driverId(), safeTraceId);
                rabbitTemplate.convertAndSend(
                    AmqpConstants.RIDE_TOPIC_EXCHANGE,
                    AmqpConstants.ROUTING_KEY_RIDE_MATCHED,
                    envelope
                );
            } catch (org.springframework.amqp.AmqpException e) {
                log.warn("Failed to publish RideMatchedEvent to RabbitMQ (broker unavailable): {}", e.getMessage());
            }
        } else {
            log.warn("RabbitTemplate is not available. Skipping publishing RideMatchedEvent for rideId {}", event.rideId());
        }
    }
}
