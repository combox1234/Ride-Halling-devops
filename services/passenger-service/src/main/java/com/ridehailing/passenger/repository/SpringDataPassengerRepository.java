package com.ridehailing.passenger.repository;

import com.ridehailing.passenger.entity.PassengerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataPassengerRepository extends JpaRepository<PassengerEntity, Long> {
    Optional<PassengerEntity> findByUserId(Long userId);
    Optional<PassengerEntity> findByEmailIgnoreCase(String email);
    boolean existsByUserId(Long userId);
    boolean existsByEmailIgnoreCase(String email);
}
