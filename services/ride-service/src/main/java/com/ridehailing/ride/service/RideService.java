package com.ridehailing.ride.service;

import com.ridehailing.common.dto.DriverCandidateDto;
import com.ridehailing.common.dto.LocationDto;
import com.ridehailing.common.dto.RideStatus;
import com.ridehailing.ride.client.DefaultDriverMatchingClient;
import com.ridehailing.ride.client.DriverMatchingClient;
import com.ridehailing.ride.dto.RideBookingRequest;
import com.ridehailing.ride.dto.RideResponse;
import com.ridehailing.ride.model.RideRecord;
import com.ridehailing.ride.repository.RideRepository;
import com.ridehailing.common.event.RideCancelledEvent;
import com.ridehailing.common.event.RideCompletedEvent;
import com.ridehailing.common.event.RideRequestedEvent;
import com.ridehailing.common.event.RideStartedEvent;
import com.ridehailing.ride.publisher.RideEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final RideStateMachine stateMachine;
    private final DriverMatchingClient matchingClient;
    private final RideEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    public RideService(
        RideRepository rideRepository,
        RideStateMachine stateMachine,
        @org.springframework.beans.factory.annotation.Autowired(required = false) DriverMatchingClient matchingClient,
        @org.springframework.beans.factory.annotation.Autowired(required = false) RideEventPublisher eventPublisher
    ) {
        this.rideRepository = rideRepository;
        this.stateMachine = stateMachine;
        this.matchingClient = matchingClient != null ? matchingClient : new DefaultDriverMatchingClient();
        this.eventPublisher = eventPublisher != null ? eventPublisher : new RideEventPublisher(null);
    }

    public RideService(
        RideRepository rideRepository,
        RideStateMachine stateMachine,
        DriverMatchingClient matchingClient
    ) {
        this(rideRepository, stateMachine, matchingClient, new RideEventPublisher(null));
    }

    public RideService(RideRepository rideRepository, RideStateMachine stateMachine) {
        this(rideRepository, stateMachine, new DefaultDriverMatchingClient(), new RideEventPublisher(null));
    }

    @Transactional
    public RideResponse requestRide(RideBookingRequest request) {
        BigDecimal estimatedFare = calculateEstimatedFare(request.pickupLocation(), request.dropoffLocation());
        Instant now = Instant.now();

        // 1. Persist initial ride in REQUESTED state
        RideRecord initialRide = new RideRecord(
            null,
            request.passengerId(),
            null,
            request.pickupLocation(),
            request.dropoffLocation(),
            estimatedFare,
            null,
            RideStatus.REQUESTED,
            null,
            now,
            null
        );
        RideRecord saved = rideRepository.save(initialRide);

        // 2. Dispatch/Match driver via matching service client
        Optional<DriverCandidateDto> candidateOpt = matchingClient.findDriverForRide(saved.id(), request.pickupLocation());
        AtomicReference<RideRecord> updatedRef = new AtomicReference<>();

        if (candidateOpt.isPresent()) {
            Long driverId = candidateOpt.get().driverId();
            rideRepository.updateAtomically(saved.id(), current -> {
                stateMachine.validateTransition(current.status(), RideStatus.DRIVER_ASSIGNED);
                RideRecord assigned = current.withStatusAndDriver(RideStatus.DRIVER_ASSIGNED, driverId);
                updatedRef.set(assigned);
                return assigned;
            });
        } else {
            rideRepository.updateAtomically(saved.id(), current -> {
                stateMachine.validateTransition(current.status(), RideStatus.SEARCHING);
                RideRecord searching = current.withStatus(RideStatus.SEARCHING);
                updatedRef.set(searching);
                return searching;
            });
        }

        RideRecord finalRecord = updatedRef.get() != null ? updatedRef.get() : saved;
        eventPublisher.publishRideRequested(new RideRequestedEvent(
            finalRecord.id(),
            finalRecord.passengerId(),
            finalRecord.pickupLocation(),
            finalRecord.dropoffLocation(),
            finalRecord.estimatedFare(),
            finalRecord.requestedAt()
        ), null);

        return toResponse(finalRecord);
    }

    @Transactional
    public RideResponse startRide(Long rideId) {
        AtomicReference<RideRecord> updatedRef = new AtomicReference<>();
        rideRepository.updateAtomically(rideId, current -> {
            stateMachine.validateTransition(current.status(), RideStatus.IN_PROGRESS);
            RideRecord updated = current.withStatusAndDriver(RideStatus.IN_PROGRESS, null);
            updatedRef.set(updated);
            return updated;
        }).orElseThrow(() -> new NoSuchElementException("Ride not found with ID: " + rideId));

        RideRecord started = updatedRef.get();
        eventPublisher.publishRideStarted(new RideStartedEvent(
            started.id(),
            started.driverId() != null ? started.driverId() : 0L,
            started.passengerId(),
            Instant.now()
        ), null);

        return toResponse(started);
    }

    @Transactional
    public RideResponse completeRide(Long rideId) {
        AtomicReference<RideRecord> resultRef = new AtomicReference<>();
        rideRepository.updateAtomically(rideId, current -> {
            // Idempotency: repeated completion returns existing completed record without side effects
            if (current.status() == RideStatus.COMPLETED) {
                resultRef.set(current);
                return current;
            }

            stateMachine.validateTransition(current.status(), RideStatus.COMPLETED);
            BigDecimal finalFare = current.estimatedFare();
            String idempotencyKey = "PAY-RIDE-" + current.id();
            Instant completedAt = Instant.now();

            RideRecord completed = current.withCompleted(finalFare, idempotencyKey, completedAt);
            resultRef.set(completed);
            return completed;
        }).orElseThrow(() -> new NoSuchElementException("Ride not found with ID: " + rideId));

        RideRecord completed = resultRef.get();
        eventPublisher.publishRideCompleted(new RideCompletedEvent(
            completed.id(),
            completed.passengerId(),
            completed.driverId() != null ? completed.driverId() : 0L,
            completed.finalFare() != null ? completed.finalFare() : completed.estimatedFare(),
            completed.idempotencyKey() != null ? completed.idempotencyKey() : ("PAY-" + completed.id()),
            completed.completedAt() != null ? completed.completedAt() : Instant.now()
        ), null);

        return toResponse(completed);
    }

    @Transactional
    public RideResponse cancelRide(Long rideId) {
        AtomicReference<RideRecord> updatedRef = new AtomicReference<>();
        rideRepository.updateAtomically(rideId, current -> {
            stateMachine.validateTransition(current.status(), RideStatus.CANCELLED);
            RideRecord cancelled = current.withCancelled();
            updatedRef.set(cancelled);
            return cancelled;
        }).orElseThrow(() -> new NoSuchElementException("Ride not found with ID: " + rideId));

        RideRecord cancelled = updatedRef.get();
        eventPublisher.publishRideCancelled(new RideCancelledEvent(
            cancelled.id(),
            cancelled.passengerId(),
            cancelled.driverId(),
            "USER_CANCELLED",
            Instant.now()
        ), null);

        return toResponse(cancelled);
    }

    public RideResponse getRide(Long rideId) {
        return toResponse(getRideRecord(rideId));
    }

    private RideRecord getRideRecord(Long rideId) {
        return rideRepository.findById(rideId)
            .orElseThrow(() -> new NoSuchElementException("Ride not found with ID: " + rideId));
    }

    private BigDecimal calculateEstimatedFare(LocationDto pickup, LocationDto dropoff) {
        // Approximate distance in kilometers using Euclidean approximation
        double latDiff = (dropoff.latitude() - pickup.latitude()) * 111.0;
        double lngDiff = (dropoff.longitude() - pickup.longitude()) * 111.0 * Math.cos(Math.toRadians(pickup.latitude()));
        double distanceKm = Math.sqrt((latDiff * latDiff) + (lngDiff * lngDiff));
        if (distanceKm < 1.0) {
            distanceKm = 1.0;
        }

        // Base fare: $5.00 + $2.50 per km
        double fare = 5.00 + (distanceKm * 2.50);
        return BigDecimal.valueOf(fare).setScale(2, RoundingMode.HALF_UP);
    }

    private RideResponse toResponse(RideRecord r) {
        return new RideResponse(
            r.id(),
            r.passengerId(),
            r.driverId(),
            r.pickupLocation(),
            r.dropoffLocation(),
            r.estimatedFare(),
            r.finalFare(),
            r.status(),
            r.idempotencyKey(),
            r.requestedAt(),
            r.completedAt()
        );
    }
}
