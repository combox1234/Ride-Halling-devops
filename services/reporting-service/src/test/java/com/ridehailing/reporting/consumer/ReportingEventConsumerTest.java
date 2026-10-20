package com.ridehailing.reporting.consumer;

import com.ridehailing.common.dto.PaymentStatus;
import com.ridehailing.common.event.EventEnvelope;
import com.ridehailing.common.event.PaymentProcessedEvent;
import com.ridehailing.reporting.service.ReportingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportingEventConsumerTest {

    @Mock
    private ReportingService reportingService;

    private ReportingEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ReportingEventConsumer(reportingService);
    }

    @Test
    void shouldDelegateEventToReportingService() {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            1L, 101L, 202L, BigDecimal.valueOf(35.00), PaymentStatus.SUCCESS, "TXN-1", "PAY-101", Instant.now()
        );
        EventEnvelope<PaymentProcessedEvent> envelope = new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "PaymentProcessedEvent",
            Instant.now(),
            "trace-rep-1",
            event
        );

        consumer.handleReportingEvent(envelope);

        verify(reportingService, times(1)).recordEvent(envelope);
    }
}
