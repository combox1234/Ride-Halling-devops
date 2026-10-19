package com.ridehailing.common.event;

import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.common.dto.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideEventsTest {

    private final LocationDto pickup = new LocationDto(37.7749, -122.4194, "Market St, SF");
    private final LocationDto dropoff = new LocationDto(37.7833, -122.4167, "Union Square, SF");

    @Test
    @DisplayName("Should create valid RideRequestedEvent")
    void shouldCreateRideRequestedEvent() {
        Instant now = Instant.now();
        RideRequestedEvent event = new RideRequestedEvent(
            1L,
            100L,
            pickup,
            dropoff,
            new BigDecimal("25.50"),
            now
        );

        assertThat(event.rideId()).isEqualTo(1L);
        assertThat(event.passengerId()).isEqualTo(100L);
        assertThat(event.pickupLocation()).isEqualTo(pickup);
        assertThat(event.dropoffLocation()).isEqualTo(dropoff);
        assertThat(event.estimatedFare()).isEqualByComparingTo("25.50");
        assertThat(event.requestedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should reject negative fare in RideRequestedEvent")
    void shouldRejectNegativeFareInRideRequestedEvent() {
        assertThatThrownBy(() -> new RideRequestedEvent(
            1L, 100L, pickup, dropoff, new BigDecimal("-1.00"), Instant.now()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("estimatedFare cannot be negative");
    }

    @Test
    @DisplayName("Should create valid RideMatchedEvent")
    void shouldCreateRideMatchedEvent() {
        Instant now = Instant.now();
        RideMatchedEvent event = new RideMatchedEvent(1L, 200L, 100L, now);

        assertThat(event.rideId()).isEqualTo(1L);
        assertThat(event.driverId()).isEqualTo(200L);
        assertThat(event.passengerId()).isEqualTo(100L);
        assertThat(event.matchedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should create valid RideStartedEvent")
    void shouldCreateRideStartedEvent() {
        Instant now = Instant.now();
        RideStartedEvent event = new RideStartedEvent(1L, 200L, 100L, now);

        assertThat(event.rideId()).isEqualTo(1L);
        assertThat(event.driverId()).isEqualTo(200L);
        assertThat(event.passengerId()).isEqualTo(100L);
        assertThat(event.startedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should create valid RideCompletedEvent")
    void shouldCreateRideCompletedEvent() {
        Instant now = Instant.now();
        RideCompletedEvent event = new RideCompletedEvent(
            1L,
            100L,
            200L,
            new BigDecimal("28.75"),
            "PAY-RIDE-1",
            now
        );

        assertThat(event.rideId()).isEqualTo(1L);
        assertThat(event.passengerId()).isEqualTo(100L);
        assertThat(event.driverId()).isEqualTo(200L);
        assertThat(event.finalFare()).isEqualByComparingTo("28.75");
        assertThat(event.idempotencyKey()).isEqualTo("PAY-RIDE-1");
        assertThat(event.completedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should reject blank idempotencyKey in RideCompletedEvent")
    void shouldRejectBlankIdempotencyKeyInRideCompletedEvent() {
        assertThatThrownBy(() -> new RideCompletedEvent(
            1L, 100L, 200L, new BigDecimal("28.75"), "   ", Instant.now()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("idempotencyKey must not be blank");
    }

    @Test
    @DisplayName("Should create valid PaymentProcessedEvent")
    void shouldCreatePaymentProcessedEvent() {
        Instant now = Instant.now();
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            50L,
            1L,
            100L,
            new BigDecimal("28.75"),
            PaymentStatus.SUCCESS,
            "TX-998811",
            "PAY-RIDE-1",
            now
        );

        assertThat(event.paymentId()).isEqualTo(50L);
        assertThat(event.rideId()).isEqualTo(1L);
        assertThat(event.passengerId()).isEqualTo(100L);
        assertThat(event.amount()).isEqualByComparingTo("28.75");
        assertThat(event.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(event.transactionRef()).isEqualTo("TX-998811");
        assertThat(event.idempotencyKey()).isEqualTo("PAY-RIDE-1");
        assertThat(event.processedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should reject negative amount in PaymentProcessedEvent")
    void shouldRejectNegativeAmountInPaymentProcessedEvent() {
        assertThatThrownBy(() -> new PaymentProcessedEvent(
            50L, 1L, 100L, new BigDecimal("-5.00"), PaymentStatus.SUCCESS, "TX-1", "KEY-1", Instant.now()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("amount cannot be negative");
    }

    @Test
    @DisplayName("Should create valid RideCancelledEvent")
    void shouldCreateRideCancelledEvent() {
        Instant now = Instant.now();
        RideCancelledEvent event = new RideCancelledEvent(1L, 100L, 200L, "Passenger cancelled", now);

        assertThat(event.rideId()).isEqualTo(1L);
        assertThat(event.passengerId()).isEqualTo(100L);
        assertThat(event.driverId()).isEqualTo(200L);
        assertThat(event.reason()).isEqualTo("Passenger cancelled");
        assertThat(event.cancelledAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should verify AmqpConstants exchange, queue, and routing key definitions")
    void shouldVerifyAmqpConstants() {
        assertThat(com.ridehailing.common.constant.AmqpConstants.RIDE_TOPIC_EXCHANGE).isEqualTo("ride.topic.exchange");
        assertThat(com.ridehailing.common.constant.AmqpConstants.BILLING_DIRECT_EXCHANGE).isEqualTo("billing.direct.exchange");
        assertThat(com.ridehailing.common.constant.AmqpConstants.DLX_TOPIC_EXCHANGE).isEqualTo("dlx.topic.exchange");
        assertThat(com.ridehailing.common.constant.AmqpConstants.QUEUE_RIDE_MATCHING).isEqualTo("q.ride.matching");
        assertThat(com.ridehailing.common.constant.AmqpConstants.QUEUE_RIDE_NOTIFICATION).isEqualTo("q.ride.notification");
        assertThat(com.ridehailing.common.constant.AmqpConstants.QUEUE_RIDE_BILLING).isEqualTo("q.ride.billing");
        assertThat(com.ridehailing.common.constant.AmqpConstants.QUEUE_DEAD_LETTER).isEqualTo("q.dead-letter");
        assertThat(com.ridehailing.common.constant.AmqpConstants.ROUTING_KEY_DLX_FAILED).isEqualTo("dlx.failed");
    }
}
