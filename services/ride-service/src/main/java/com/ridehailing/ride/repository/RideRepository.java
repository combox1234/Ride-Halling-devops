package com.ridehailing.ride.repository;

import com.ridehailing.ride.entity.RideEntity;
import com.ridehailing.ride.model.RideRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

@Repository
public class RideRepository {

    private final SpringDataRideRepository springDataRideRepository;
    private final ConcurrentHashMap<Long, RideRecord> inMemoryStore;
    private final AtomicLong inMemoryIdSequence;

    public RideRepository() {
        this(null);
    }

    @Autowired
    public RideRepository(@Autowired(required = false) SpringDataRideRepository springDataRideRepository) {
        this.springDataRideRepository = springDataRideRepository;
        if (springDataRideRepository == null) {
            this.inMemoryStore = new ConcurrentHashMap<>();
            this.inMemoryIdSequence = new AtomicLong(500);
        } else {
            this.inMemoryStore = null;
            this.inMemoryIdSequence = null;
        }
    }

    @Transactional
    public RideRecord save(RideRecord ride) {
        if (springDataRideRepository != null) {
            RideEntity entity = RideEntity.fromModel(ride);
            RideEntity saved = springDataRideRepository.save(entity);
            return saved.toModel();
        }
        Long id = ride.id();
        if (id == null) {
            id = inMemoryIdSequence.incrementAndGet();
            ride = new RideRecord(
                id,
                ride.passengerId(),
                ride.driverId(),
                ride.pickupLocation(),
                ride.dropoffLocation(),
                ride.estimatedFare(),
                ride.finalFare(),
                ride.status(),
                ride.idempotencyKey(),
                ride.requestedAt(),
                ride.completedAt()
            );
        }
        inMemoryStore.put(id, ride);
        return ride;
    }

    public Optional<RideRecord> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        if (springDataRideRepository != null) {
            return springDataRideRepository.findById(id).map(RideEntity::toModel);
        }
        return Optional.ofNullable(inMemoryStore.get(id));
    }

    @Transactional
    public Optional<RideRecord> updateAtomically(Long id, UnaryOperator<RideRecord> updater) {
        if (id == null) {
            return Optional.empty();
        }
        if (springDataRideRepository != null) {
            return springDataRideRepository.findById(id).map(entity -> {
                RideRecord current = entity.toModel();
                RideRecord updated = updater.apply(current);
                entity.updateFromModel(updated);
                RideEntity saved = springDataRideRepository.saveAndFlush(entity);
                return saved.toModel();
            });
        }
        return Optional.ofNullable(inMemoryStore.computeIfPresent(id, (k, current) -> updater.apply(current)));
    }

    public List<RideRecord> findAllByPassengerId(Long passengerId) {
        if (passengerId == null) {
            return List.of();
        }
        if (springDataRideRepository != null) {
            return springDataRideRepository.findAllByPassengerId(passengerId).stream()
                .map(RideEntity::toModel)
                .toList();
        }
        return inMemoryStore.values().stream()
            .filter(r -> r.passengerId().equals(passengerId))
            .toList();
    }

    public Optional<RideRecord> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        if (springDataRideRepository != null) {
            return springDataRideRepository.findByIdempotencyKey(idempotencyKey).map(RideEntity::toModel);
        }
        return inMemoryStore.values().stream()
            .filter(r -> idempotencyKey.equals(r.idempotencyKey()))
            .findFirst();
    }

    public void clear() {
        if (springDataRideRepository != null) {
            springDataRideRepository.deleteAll();
        } else {
            inMemoryStore.clear();
            inMemoryIdSequence.set(500);
        }
    }

    public long count() {
        if (springDataRideRepository != null) {
            return springDataRideRepository.count();
        }
        return inMemoryStore.size();
    }
}
