package com.ridehailing.passenger.service;

import com.ridehailing.passenger.dto.CreatePassengerRequest;
import com.ridehailing.passenger.dto.UpdatePassengerRequest;
import com.ridehailing.passenger.model.PassengerProfile;
import com.ridehailing.passenger.repository.PassengerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PassengerServiceTest {

    private PassengerRepository passengerRepository;
    private PassengerService passengerService;

    @BeforeEach
    void setUp() {
        passengerRepository = new PassengerRepository();
        passengerService = new PassengerService(passengerRepository);
    }

    @Test
    @DisplayName("Should create passenger profile successfully")
    void shouldCreatePassengerProfile() {
        CreatePassengerRequest req = new CreatePassengerRequest(101L, "Bob Passenger", "bob@example.com", "+1234567890");
        PassengerProfile profile = passengerService.createProfile(req);

        assertThat(profile.id()).isNotNull();
        assertThat(profile.userId()).isEqualTo(101L);
        assertThat(profile.name()).isEqualTo("Bob Passenger");
        assertThat(profile.email()).isEqualTo("bob@example.com");
        assertThat(profile.rating()).isEqualTo(5.0);
        assertThat(profile.totalRides()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should reject profile creation if userId already has profile")
    void shouldRejectDuplicateUserId() {
        CreatePassengerRequest req1 = new CreatePassengerRequest(101L, "Bob", "bob@example.com", "+1234567890");
        passengerService.createProfile(req1);

        CreatePassengerRequest req2 = new CreatePassengerRequest(101L, "Bob Two", "bob2@example.com", "+1234567891");
        assertThatThrownBy(() -> passengerService.createProfile(req2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Passenger profile already exists");
    }

    @Test
    @DisplayName("Should retrieve profile by ID")
    void shouldGetProfileById() {
        CreatePassengerRequest req = new CreatePassengerRequest(102L, "Alice", "alice@example.com", "+1987654321");
        PassengerProfile created = passengerService.createProfile(req);

        PassengerProfile found = passengerService.getById(created.id());
        assertThat(found.email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when profile ID not found")
    void shouldThrowWhenIdNotFound() {
        assertThatThrownBy(() -> passengerService.getById(999L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Passenger not found");
    }

    @Test
    @DisplayName("Should update passenger name and phone")
    void shouldUpdateProfile() {
        CreatePassengerRequest req = new CreatePassengerRequest(103L, "Carol", "carol@example.com", "+1112223333");
        PassengerProfile created = passengerService.createProfile(req);

        PassengerProfile updated = passengerService.updateProfile(created.id(), new UpdatePassengerRequest("Carol New", "+1999888777"));
        assertThat(updated.name()).isEqualTo("Carol New");
        assertThat(updated.phone()).isEqualTo("+1999888777");
    }
}
