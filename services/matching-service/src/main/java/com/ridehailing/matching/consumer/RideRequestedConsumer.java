package com.ridehailing.matching.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.common.constant.AmqpConstants;
import com.ridehailing.common.dto.DriverCandidateDto;
import com.ridehailing.common.dto.DriverMatchRequest;
import com.ridehailing.common.event.EventEnvelope;
import com.ridehailing.common.event.RideMatchedEvent;
import com.ridehailing.common.event.RideRequestedEvent;
import com.ridehailing.matching.publisher.MatchingEventPublisher;
import com.ridehailing.matching.service.MatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Component
public class RideRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RideRequestedConsumer.class);

    private final MatchingService matchingService;
    private final MatchingEventPublisher matchingEventPublisher;
    private final ObjectMapper objectMapper;

    public RideRequestedConsumer(
        MatchingService matchingService,
        MatchingEventPublisher matchingEventPublisher,
        ObjectMapper objectMapper
    ) {
        this.matchingService = matchingService;
        this.matchingEventPublisher = matchingEventPublisher;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = AmqpConstants.QUEUE_RIDE_MATCHING)
    public void handleRideRequested(EventEnvelope<?> envelope) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        log.info("Received event {} [id={}] with traceId {}",
            envelope.eventType(), envelope.eventId(), envelope.traceId());

        RideRequestedEvent event = extractEvent(envelope);
        if (event == null) {
            log.warn("Unable to extract RideRequestedEvent from payload: {}", envelope.payload());
            return;
        }

        processRideRequested(event, envelope.traceId());
    }

    public void processRideRequested(RideRequestedEvent event, String traceId) {
        log.info("Processing ride request match for rideId={}, passengerId={}, pickup=({}, {})",
            event.rideId(), event.passengerId(),
            event.pickupLocation().latitude(), event.pickupLocation().longitude());

        DriverMatchRequest matchRequest = new DriverMatchRequest(
            event.rideId(),
            event.pickupLocation(),
            5.0
        );

        Optional<DriverCandidateDto> candidate = matchingService.findBestDriver(matchRequest);
        if (candidate.isPresent()) {
            DriverCandidateDto driver = candidate.get();
            log.info("Driver {} matched for ride {}", driver.driverId(), event.rideId());

            RideMatchedEvent matchedEvent = new RideMatchedEvent(
                event.rideId(),
                driver.driverId(),
                event.passengerId(),
                Instant.now()
            );

            matchingEventPublisher.publishRideMatched(matchedEvent, traceId);
        } else {
            log.warn("No available drivers found for ride {}", event.rideId());
        }
    }

    private RideRequestedEvent extractEvent(EventEnvelope<?> envelope) {
        if (envelope.payload() instanceof RideRequestedEvent direct) {
            return direct;
        }
        try {
            return objectMapper.convertValue(envelope.payload(), RideRequestedEvent.class);
        } catch (Exception e) {
            log.error("Failed to convert payload to RideRequestedEvent: {}", envelope.payload(), e);
            throw new IllegalArgumentException("Payload could not be mapped to RideRequestedEvent", e);
        }
    }
}
