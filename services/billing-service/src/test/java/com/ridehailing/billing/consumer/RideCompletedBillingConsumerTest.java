package com.ridehailing.billing.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridehailing.billing.dto.PaymentReceiptResponse;
import com.ridehailing.billing.dto.ProcessPaymentRequest;
import com.ridehailing.billing.service.BillingService;
import com.ridehailing.common.dto.PaymentStatus;
import com.ridehailing.common.event.EventEnvelope;
import com.ridehailing.common.event.RideCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideCompletedBillingConsumerTest {

    @Mock
    private BillingService billingService;

    private ObjectMapper objectMapper;
    private RideCompletedBillingConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new RideCompletedBillingConsumer(billingService, objectMapper);
    }

    @Test
    void shouldConsumeRideCompletedEventAndTriggerPayment() {
        RideCompletedEvent rideCompletedEvent = new RideCompletedEvent(
            500L,
            202L,
            303L,
            BigDecimal.valueOf(42.75),
            "PAY-RIDE-500",
            Instant.now()
        );

        EventEnvelope<RideCompletedEvent> envelope = new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "RideCompletedEvent",
            Instant.now(),
            "trace-bill-1",
            rideCompletedEvent
        );

        PaymentReceiptResponse receipt = new PaymentReceiptResponse(
            1L, 500L, 202L, BigDecimal.valueOf(42.75), PaymentStatus.SUCCESS,
            "CREDIT_CARD", "TXN-1", "PAY-RIDE-500", null, null, "INV-500", Instant.now()
        );
        when(billingService.processPayment(any(ProcessPaymentRequest.class))).thenReturn(receipt);

        consumer.handleRideCompletedEvent(envelope);

        ArgumentCaptor<ProcessPaymentRequest> captor = ArgumentCaptor.forClass(ProcessPaymentRequest.class);
        verify(billingService, times(1)).processPayment(captor.capture());

        ProcessPaymentRequest captured = captor.getValue();
        assertEquals(500L, captured.rideId());
        assertEquals(202L, captured.passengerId());
        assertEquals(BigDecimal.valueOf(42.75), captured.amount());
        assertEquals("PAY-RIDE-500", captured.idempotencyKey());
    }

    @Test
    void shouldIgnoreNonRideCompletedEvents() {
        EventEnvelope<String> envelope = new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "OtherEvent",
            Instant.now(),
            "trace-bill-2",
            "dummy"
        );

        consumer.handleRideCompletedEvent(envelope);

        verify(billingService, never()).processPayment(any());
    }
}
