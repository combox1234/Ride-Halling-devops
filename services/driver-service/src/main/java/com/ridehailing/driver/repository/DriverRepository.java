package com.ridehailing.driver.repository;

import com.ridehailing.driver.entity.DriverEntity;
import com.ridehailing.driver.model.DriverProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class DriverRepository {

    private final SpringDataDriverRepository springDataDriverRepository;
    private final ConcurrentHashMap<Long, DriverProfile> inMemoryStore;
    private final ConcurrentHashMap<Long, Long> inMemoryUserIdIndex;
    private final ConcurrentHashMap<String, Long> inMemoryEmailIndex;
    private final ConcurrentHashMap<String, Long> inMemoryPlateIndex;
    private final AtomicLong inMemoryIdSequence;

    public DriverRepository() {
        this(null);
    }

    @Autowired
    public DriverRepository(@Autowired(required = false) SpringDataDriverRepository springDataDriverRepository) {
        this.springDataDriverRepository = springDataDriverRepository;
        if (springDataDriverRepository == null) {
            this.inMemoryStore = new ConcurrentHashMap<>();
            this.inMemoryUserIdIndex = new ConcurrentHashMap<>();
            this.inMemoryEmailIndex = new ConcurrentHashMap<>();
            this.inMemoryPlateIndex = new ConcurrentHashMap<>();
            this.inMemoryIdSequence = new AtomicLong(200);
        } else {
            this.inMemoryStore = null;
            this.inMemoryUserIdIndex = null;
            this.inMemoryEmailIndex = null;
            this.inMemoryPlateIndex = null;
            this.inMemoryIdSequence = null;
        }
    }

    public DriverProfile save(DriverProfile driver) {
        if (springDataDriverRepository != null) {
            DriverEntity entity = DriverEntity.fromModel(driver);
            DriverEntity saved = springDataDriverRepository.save(entity);
            return saved.toModel();
        }
        Long id = driver.id();
        if (id == null) {
            id = inMemoryIdSequence.incrementAndGet();
            driver = new DriverProfile(
                id,
                driver.userId(),
                driver.name(),
                driver.email(),
                driver.phone(),
                driver.vehicleMake(),
                driver.vehicleModel(),
                driver.licensePlate(),
                driver.status(),
                driver.rating(),
                driver.totalTrips(),
                driver.createdAt()
            );
        }
        inMemoryStore.put(id, driver);
        inMemoryUserIdIndex.put(driver.userId(), id);
        inMemoryEmailIndex.put(driver.email().toLowerCase().trim(), id);
        inMemoryPlateIndex.put(driver.licensePlate().toUpperCase().trim(), id);
        return driver;
    }

    public Optional<DriverProfile> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        if (springDataDriverRepository != null) {
            return springDataDriverRepository.findById(id).map(DriverEntity::toModel);
        }
        return Optional.ofNullable(inMemoryStore.get(id));
    }

    public Optional<DriverProfile> findByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        if (springDataDriverRepository != null) {
            return springDataDriverRepository.findByUserId(userId).map(DriverEntity::toModel);
        }
        Long id = inMemoryUserIdIndex.get(userId);
        return id != null ? Optional.ofNullable(inMemoryStore.get(id)) : Optional.empty();
    }

    public Optional<DriverProfile> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String normalized = email.toLowerCase().trim();
        if (springDataDriverRepository != null) {
            return springDataDriverRepository.findByEmailIgnoreCase(normalized).map(DriverEntity::toModel);
        }
        Long id = inMemoryEmailIndex.get(normalized);
        return id != null ? Optional.ofNullable(inMemoryStore.get(id)) : Optional.empty();
    }

    public boolean existsByUserId(Long userId) {
        if (userId == null) {
            return false;
        }
        if (springDataDriverRepository != null) {
            return springDataDriverRepository.existsByUserId(userId);
        }
        return inMemoryUserIdIndex.containsKey(userId);
    }

    public boolean existsByEmail(String email) {
        if (email == null) {
            return false;
        }
        String normalized = email.toLowerCase().trim();
        if (springDataDriverRepository != null) {
            return springDataDriverRepository.existsByEmailIgnoreCase(normalized);
        }
        return inMemoryEmailIndex.containsKey(normalized);
    }

    public boolean existsByLicensePlate(String licensePlate) {
        if (licensePlate == null) {
            return false;
        }
        String normalized = licensePlate.toUpperCase().trim();
        if (springDataDriverRepository != null) {
            return springDataDriverRepository.existsByLicensePlateIgnoreCase(normalized);
        }
        return inMemoryPlateIndex.containsKey(normalized);
    }

    public void clear() {
        if (springDataDriverRepository != null) {
            springDataDriverRepository.deleteAll();
        } else {
            inMemoryStore.clear();
            inMemoryUserIdIndex.clear();
            inMemoryEmailIndex.clear();
            inMemoryPlateIndex.clear();
            inMemoryIdSequence.set(200);
        }
    }

    public long count() {
        if (springDataDriverRepository != null) {
            return springDataDriverRepository.count();
        }
        return inMemoryStore.size();
    }
}
