package com.ridehailing.passenger.service;

import com.ridehailing.passenger.dto.CreatePassengerRequest;
import com.ridehailing.passenger.dto.UpdatePassengerRequest;
import com.ridehailing.passenger.model.PassengerProfile;
import com.ridehailing.passenger.repository.PassengerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;

@Service
public class PassengerService {

    private final PassengerRepository passengerRepository;

    public PassengerService(PassengerRepository passengerRepository) {
        this.passengerRepository = passengerRepository;
    }

    @Transactional
    public PassengerProfile createProfile(CreatePassengerRequest request) {
        if (passengerRepository.existsByUserId(request.userId())) {
            throw new IllegalArgumentException("Passenger profile already exists for user ID: " + request.userId());
        }
        if (passengerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        PassengerProfile profile = new PassengerProfile(
            null,
            request.userId(),
            request.name().trim(),
            request.email().toLowerCase().trim(),
            request.phone().trim(),
            5.0, // Default starting rating
            0,
            Instant.now()
        );

        return passengerRepository.save(profile);
    }

    public PassengerProfile getById(Long id) {
        return passengerRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Passenger not found with ID: " + id));
    }

    public PassengerProfile getByUserId(Long userId) {
        return passengerRepository.findByUserId(userId)
            .orElseThrow(() -> new NoSuchElementException("Passenger profile not found for user ID: " + userId));
    }

    @Transactional
    public PassengerProfile updateProfile(Long id, UpdatePassengerRequest request) {
        PassengerProfile existing = getById(id);
        PassengerProfile updated = existing.withUpdatedInfo(request.name(), request.phone());
        return passengerRepository.save(updated);
    }

    @Transactional
    public PassengerProfile recordCompletedRide(Long id) {
        PassengerProfile existing = getById(id);
        PassengerProfile updated = existing.withIncrementedRides();
        return passengerRepository.save(updated);
    }
}
