package com.ridehailing.passenger.repository;

import com.ridehailing.passenger.entity.PassengerEntity;
import com.ridehailing.passenger.model.PassengerProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class PassengerRepository {

    private final SpringDataPassengerRepository springDataPassengerRepository;
    private final ConcurrentHashMap<Long, PassengerProfile> inMemoryStore;
    private final ConcurrentHashMap<Long, Long> inMemoryUserIdIndex;
    private final ConcurrentHashMap<String, Long> inMemoryEmailIndex;
    private final AtomicLong inMemoryIdSequence;

    public PassengerRepository() {
        this(null);
    }

    @Autowired
    public PassengerRepository(@Autowired(required = false) SpringDataPassengerRepository springDataPassengerRepository) {
        this.springDataPassengerRepository = springDataPassengerRepository;
        if (springDataPassengerRepository == null) {
            this.inMemoryStore = new ConcurrentHashMap<>();
            this.inMemoryUserIdIndex = new ConcurrentHashMap<>();
            this.inMemoryEmailIndex = new ConcurrentHashMap<>();
            this.inMemoryIdSequence = new AtomicLong(100);
        } else {
            this.inMemoryStore = null;
            this.inMemoryUserIdIndex = null;
            this.inMemoryEmailIndex = null;
            this.inMemoryIdSequence = null;
        }
    }

    public PassengerProfile save(PassengerProfile passenger) {
        if (springDataPassengerRepository != null) {
            PassengerEntity entity = PassengerEntity.fromModel(passenger);
            PassengerEntity saved = springDataPassengerRepository.save(entity);
            return saved.toModel();
        }
        Long id = passenger.id();
        if (id == null) {
            id = inMemoryIdSequence.incrementAndGet();
            passenger = new PassengerProfile(
                id,
                passenger.userId(),
                passenger.name(),
                passenger.email(),
                passenger.phone(),
                passenger.rating(),
                passenger.totalRides(),
                passenger.createdAt()
            );
        }
        inMemoryStore.put(id, passenger);
        inMemoryUserIdIndex.put(passenger.userId(), id);
        inMemoryEmailIndex.put(passenger.email().toLowerCase().trim(), id);
        return passenger;
    }

    public Optional<PassengerProfile> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        if (springDataPassengerRepository != null) {
            return springDataPassengerRepository.findById(id).map(PassengerEntity::toModel);
        }
        return Optional.ofNullable(inMemoryStore.get(id));
    }

    public Optional<PassengerProfile> findByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        if (springDataPassengerRepository != null) {
            return springDataPassengerRepository.findByUserId(userId).map(PassengerEntity::toModel);
        }
        Long id = inMemoryUserIdIndex.get(userId);
        return id != null ? Optional.ofNullable(inMemoryStore.get(id)) : Optional.empty();
    }

    public Optional<PassengerProfile> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String normalized = email.toLowerCase().trim();
        if (springDataPassengerRepository != null) {
            return springDataPassengerRepository.findByEmailIgnoreCase(normalized).map(PassengerEntity::toModel);
        }
        Long id = inMemoryEmailIndex.get(normalized);
        return id != null ? Optional.ofNullable(inMemoryStore.get(id)) : Optional.empty();
    }

    public boolean existsByUserId(Long userId) {
        if (userId == null) {
            return false;
        }
        if (springDataPassengerRepository != null) {
            return springDataPassengerRepository.existsByUserId(userId);
        }
        return inMemoryUserIdIndex.containsKey(userId);
    }

    public boolean existsByEmail(String email) {
        if (email == null) {
            return false;
        }
        String normalized = email.toLowerCase().trim();
        if (springDataPassengerRepository != null) {
            return springDataPassengerRepository.existsByEmailIgnoreCase(normalized);
        }
        return inMemoryEmailIndex.containsKey(normalized);
    }

    public void clear() {
        if (springDataPassengerRepository != null) {
            springDataPassengerRepository.deleteAll();
        } else {
            inMemoryStore.clear();
            inMemoryUserIdIndex.clear();
            inMemoryEmailIndex.clear();
            inMemoryIdSequence.set(100);
        }
    }

    public long count() {
        if (springDataPassengerRepository != null) {
            return springDataPassengerRepository.count();
        }
        return inMemoryStore.size();
    }
}
