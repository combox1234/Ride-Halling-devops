package com.ridehailing.ride.repository;

import com.ridehailing.ride.entity.RideEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataRideRepository extends JpaRepository<RideEntity, Long> {
    List<RideEntity> findAllByPassengerId(Long passengerId);
    Optional<RideEntity> findByIdempotencyKey(String idempotencyKey);
}
