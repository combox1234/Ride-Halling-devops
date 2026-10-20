package com.ridehailing.billing.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.billing.dto.ProcessPaymentRequest;
import com.ridehailing.billing.service.BillingService;
import com.ridehailing.common.constant.AmqpConstants;
import com.ridehailing.common.event.EventEnvelope;
import com.ridehailing.common.event.RideCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class RideCompletedBillingConsumer {

    private static final Logger log = LoggerFactory.getLogger(RideCompletedBillingConsumer.class);

    private final BillingService billingService;
    private final ObjectMapper objectMapper;

    public RideCompletedBillingConsumer(BillingService billingService, ObjectMapper objectMapper) {
        this.billingService = billingService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = AmqpConstants.QUEUE_RIDE_BILLING)
    public void handleRideCompletedEvent(EventEnvelope<?> envelope) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        log.info("Billing consumer received event {} [id={}] with traceId {}",
            envelope.eventType(), envelope.eventId(), envelope.traceId());

        processEnvelope(envelope);
    }

    public void processEnvelope(EventEnvelope<?> envelope) {
        if ("RideCompletedEvent".equals(envelope.eventType())) {
            RideCompletedEvent event = convertPayload(envelope.payload(), RideCompletedEvent.class);
            log.info("Processing billing for completed ride #{} with finalFare=${}", event.rideId(), event.finalFare());

            ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest(
                event.rideId(),
                event.passengerId(),
                event.finalFare(),
                event.idempotencyKey(),
                "CREDIT_CARD"
            );

            billingService.processPayment(paymentRequest);
        } else {
            log.warn("Ignored unexpected event type in billing consumer: {}", envelope.eventType());
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
}
