package com.ridehailing.ride.service;

import com.ridehailing.common.dto.RideStatus;
import com.ridehailing.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideStateMachineTest {

    private RideStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new RideStateMachine();
    }

    @ParameterizedTest
    @CsvSource({
        "REQUESTED, DRIVER_ASSIGNED",
        "REQUESTED, CANCELLED",
        "DRIVER_ASSIGNED, IN_PROGRESS",
        "DRIVER_ASSIGNED, CANCELLED",
        "IN_PROGRESS, COMPLETED"
    })
    @DisplayName("Should allow valid state machine transitions")
    void shouldAllowValidTransitions(RideStatus from, RideStatus to) {
        assertThatCode(() -> stateMachine.validateTransition(from, to))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject cancellation of IN_PROGRESS ride")
    void shouldRejectCancellationWhenInProgress() {
        assertThatThrownBy(() -> stateMachine.validateTransition(RideStatus.IN_PROGRESS, RideStatus.CANCELLED))
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining("Invalid ride state transition");
    }

    @Test
    @DisplayName("Should reject any transition out of COMPLETED terminal state")
    void shouldRejectTransitionFromCompleted() {
        assertThatThrownBy(() -> stateMachine.validateTransition(RideStatus.COMPLETED, RideStatus.IN_PROGRESS))
            .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Should reject any transition out of CANCELLED terminal state")
    void shouldRejectTransitionFromCancelled() {
        assertThatThrownBy(() -> stateMachine.validateTransition(RideStatus.CANCELLED, RideStatus.REQUESTED))
            .isInstanceOf(InvalidStateTransitionException.class);
    }
}
