package com.ridehailing.rating.repository;

import com.ridehailing.rating.entity.RatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<RatingEntity, Long> {

    Optional<RatingEntity> findByRideIdAndRaterId(Long rideId, Long raterId);

    List<RatingEntity> findByRateeId(Long rateeId);

    List<RatingEntity> findByRideId(Long rideId);

    boolean existsByRideIdAndRaterId(Long rideId, Long raterId);
}
