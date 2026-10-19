package com.ridehailing.ride.service;

import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.common.dto.RideStatus;
import com.ridehailing.common.exception.InvalidStateTransitionException;
import com.ridehailing.ride.dto.RideBookingRequest;
import com.ridehailing.ride.dto.RideResponse;
import com.ridehailing.ride.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideServiceTest {

    private RideRepository rideRepository;
    private RideStateMachine stateMachine;
    private RideService rideService;

    private final LocationDto pickup = new LocationDto(37.7749, -122.4194, "Market St, San Francisco");
    private final LocationDto dropoff = new LocationDto(37.7892, -122.4014, "Financial District, San Francisco");

    @BeforeEach
    void setUp() {
        rideRepository = new RideRepository();
        stateMachine = new RideStateMachine();
        rideService = new RideService(rideRepository, stateMachine);
    }

    @Test
    @DisplayName("Should request ride and perform mock driver assignment")
    void shouldRequestRideAndAssignDriver() {
        RideBookingRequest request = new RideBookingRequest(101L, pickup, dropoff);
        RideResponse response = rideService.requestRide(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.passengerId()).isEqualTo(101L);
        assertThat(response.driverId()).isEqualTo(201L);
        assertThat(response.status()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
        assertThat(response.estimatedFare()).isPositive();
    }

    @Test
    @DisplayName("Should progress ride from DRIVER_ASSIGNED to IN_PROGRESS")
    void shouldStartRide() {
        RideBookingRequest request = new RideBookingRequest(101L, pickup, dropoff);
        RideResponse created = rideService.requestRide(request);

        RideResponse started = rideService.startRide(created.id());
        assertThat(started.status()).isEqualTo(RideStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should complete ride and generate payment idempotency key")
    void shouldCompleteRideAndGenerateIdempotencyKey() {
        RideBookingRequest request = new RideBookingRequest(101L, pickup, dropoff);
        RideResponse created = rideService.requestRide(request);
        rideService.startRide(created.id());

        RideResponse completed = rideService.completeRide(created.id());
        assertThat(completed.status()).isEqualTo(RideStatus.COMPLETED);
        assertThat(completed.finalFare()).isNotNull();
        assertThat(completed.idempotencyKey()).isEqualTo("PAY-RIDE-" + created.id());
        assertThat(completed.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("Idempotency: Repeated completeRide calls return identical completed result without side effects")
    void shouldReturnIdempotentResultOnRepeatedCompletion() {
        RideBookingRequest request = new RideBookingRequest(101L, pickup, dropoff);
        RideResponse created = rideService.requestRide(request);
        rideService.startRide(created.id());

        RideResponse firstCompletion = rideService.completeRide(created.id());
        RideResponse secondCompletion = rideService.completeRide(created.id());

        assertThat(secondCompletion.status()).isEqualTo(RideStatus.COMPLETED);
        assertThat(secondCompletion.idempotencyKey()).isEqualTo(firstCompletion.idempotencyKey());
        assertThat(secondCompletion.finalFare()).isEqualTo(firstCompletion.finalFare());
        assertThat(secondCompletion.completedAt()).isEqualTo(firstCompletion.completedAt());
    }

    @Test
    @DisplayName("Should reject cancellation when ride is already IN_PROGRESS")
    void shouldRejectCancelWhenInProgress() {
        RideBookingRequest request = new RideBookingRequest(101L, pickup, dropoff);
        RideResponse created = rideService.requestRide(request);
        rideService.startRide(created.id());

        assertThatThrownBy(() -> rideService.cancelRide(created.id()))
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining("Invalid ride state transition");
    }

    @Test
    @DisplayName("Should cancel ride successfully if DRIVER_ASSIGNED")
    void shouldCancelRideWhenAssigned() {
        RideBookingRequest request = new RideBookingRequest(101L, pickup, dropoff);
        RideResponse created = rideService.requestRide(request);

        RideResponse cancelled = rideService.cancelRide(created.id());
        assertThat(cancelled.status()).isEqualTo(RideStatus.CANCELLED);
    }

    @Test
    @DisplayName("Concurrency: Simultaneous racing start and cancel operations must execute atomically")
    void shouldHandleConcurrentTransitionsAtomically() throws Exception {
        RideBookingRequest request = new RideBookingRequest(101L, pickup, dropoff);
        RideResponse created = rideService.requestRide(request);
        Long rideId = created.id();

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger startSuccesses = new AtomicInteger(0);
        AtomicInteger cancelSuccesses = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final boolean tryStart = (i % 2 == 0);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (tryStart) {
                        rideService.startRide(rideId);
                        startSuccesses.incrementAndGet();
                    } else {
                        rideService.cancelRide(rideId);
                        cancelSuccesses.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    // Expected when losing the state transition race
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // The ride must end in either IN_PROGRESS or CANCELLED, never in an undefined state
        RideResponse finalState = rideService.getRide(rideId);
        assertThat(finalState.status()).isIn(RideStatus.IN_PROGRESS, RideStatus.CANCELLED);

        // One mutually exclusive action won; the other could not simultaneously corrupt the record
        if (finalState.status() == RideStatus.IN_PROGRESS) {
            assertThat(cancelSuccesses.get()).isZero();
            assertThat(startSuccesses.get()).isGreaterThanOrEqualTo(1);
        } else {
            assertThat(startSuccesses.get()).isZero();
            assertThat(cancelSuccesses.get()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    @DisplayName("Should transition to SEARCHING when no nearby driver is available")
    void shouldTransitionToSearchingWhenNoDriverFound() {
        RideService serviceWithNoDrivers = new RideService(
            rideRepository,
            stateMachine,
            (rideId, pickupLocation) -> java.util.Optional.empty()
        );

        RideBookingRequest request = new RideBookingRequest(101L, pickup, dropoff);
        RideResponse response = serviceWithNoDrivers.requestRide(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.driverId()).isNull();
        assertThat(response.status()).isEqualTo(RideStatus.SEARCHING);
    }

    @Test
    @DisplayName("Should assign specific matched driver returned by matching client")
    void shouldAssignMatchedDriverFromClient() {
        RideService serviceWithMatchedDriver = new RideService(
            rideRepository,
            stateMachine,
            (rideId, pickupLocation) -> java.util.Optional.of(
                new com.ridehailing.common.dto.DriverCandidateDto(305L, 0.8, pickupLocation.latitude(), pickupLocation.longitude(), 4.9)
            )
        );

        RideBookingRequest request = new RideBookingRequest(101L, pickup, dropoff);
        RideResponse response = serviceWithMatchedDriver.requestRide(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.driverId()).isEqualTo(305L);
        assertThat(response.status()).isEqualTo(RideStatus.DRIVER_ASSIGNED);
    }
}
