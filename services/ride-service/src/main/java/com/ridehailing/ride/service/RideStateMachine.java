package com.ridehailing.ride.service;

import com.ridehailing.common.dto.RideStatus;
import com.ridehailing.common.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class RideStateMachine {

    private static final Map<RideStatus, Set<RideStatus>> VALID_TRANSITIONS = Map.of(
        RideStatus.REQUESTED, Set.of(RideStatus.PRICE_ESTIMATED, RideStatus.SEARCHING, RideStatus.DRIVER_ASSIGNED, RideStatus.CANCELLED),
        RideStatus.PRICE_ESTIMATED, Set.of(RideStatus.SEARCHING, RideStatus.CANCELLED),
        RideStatus.SEARCHING, Set.of(RideStatus.DRIVER_ASSIGNED, RideStatus.CANCELLED),
        RideStatus.DRIVER_ASSIGNED, Set.of(RideStatus.DRIVER_ARRIVED, RideStatus.IN_PROGRESS, RideStatus.CANCELLED),
        RideStatus.DRIVER_ARRIVED, Set.of(RideStatus.IN_PROGRESS, RideStatus.CANCELLED),
        RideStatus.IN_PROGRESS, Set.of(RideStatus.COMPLETED),
        RideStatus.COMPLETED, Set.of(),
        RideStatus.CANCELLED, Set.of()
    );

    public void validateTransition(RideStatus currentStatus, RideStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new IllegalArgumentException("Current status and target status must not be null");
        }

        Set<RideStatus> allowedTargets = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTargets.contains(targetStatus)) {
            throw new InvalidStateTransitionException(
                "Invalid ride state transition from " + currentStatus + " to " + targetStatus +
                ". In-progress or completed rides cannot be cancelled."
            );
        }
    }
}
