package com.ridehailing.reporting.consumer;

import com.ridehailing.common.constant.AmqpConstants;
import com.ridehailing.common.event.EventEnvelope;
import com.ridehailing.reporting.service.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ReportingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReportingEventConsumer.class);

    private final ReportingService reportingService;

    public ReportingEventConsumer(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @RabbitListener(queues = {AmqpConstants.QUEUE_RIDE_REPORTING, AmqpConstants.QUEUE_BILLING_REPORTING})
    public void handleReportingEvent(EventEnvelope<?> envelope) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        log.info("Reporting event consumer received {} [id={}] with traceId {}",
            envelope.eventType(), envelope.eventId(), envelope.traceId());

        reportingService.recordEvent(envelope);
    }
}
