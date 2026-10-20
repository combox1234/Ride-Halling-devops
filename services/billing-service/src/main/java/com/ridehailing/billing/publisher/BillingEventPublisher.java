package com.ridehailing.billing.publisher;

import com.ridehailing.common.constant.AmqpConstants;
import com.ridehailing.common.event.EventEnvelope;
import com.ridehailing.common.event.PaymentProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class BillingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BillingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public BillingEventPublisher() {
        this(null);
    }

    @Autowired(required = false)
    public BillingEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentProcessed(PaymentProcessedEvent event, String traceId) {
        String effectiveTraceId = (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
        EventEnvelope<PaymentProcessedEvent> envelope = new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "PaymentProcessedEvent",
            Instant.now(),
            effectiveTraceId,
            event
        );

        if (rabbitTemplate == null) {
            log.info("RabbitTemplate not available; skipping AMQP publish for payment processed event: {}", event);
            return;
        }

        try {
            log.info("Publishing payment processed event to exchange '{}' [key='{}']: {}",
                AmqpConstants.BILLING_DIRECT_EXCHANGE, AmqpConstants.ROUTING_KEY_BILLING_PROCESSED, event);
            rabbitTemplate.convertAndSend(
                AmqpConstants.BILLING_DIRECT_EXCHANGE,
                AmqpConstants.ROUTING_KEY_BILLING_PROCESSED,
                envelope
            );
        } catch (AmqpException ex) {
            log.warn("Failed to publish payment processed event to RabbitMQ (broker offline or unreachable): {}", ex.getMessage());
        }
    }
}
