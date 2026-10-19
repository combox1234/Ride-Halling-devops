package com.ridehailing.auth.repository;

import com.ridehailing.auth.entity.UserEntity;
import com.ridehailing.auth.model.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final ConcurrentHashMap<Long, UserAccount> inMemoryStore;
    private final ConcurrentHashMap<String, Long> inMemoryEmailIndex;
    private final AtomicLong inMemoryIdSequence;

    public UserRepository() {
        this(null);
    }

    @Autowired
    public UserRepository(@Autowired(required = false) SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
        if (springDataUserRepository == null) {
            this.inMemoryStore = new ConcurrentHashMap<>();
            this.inMemoryEmailIndex = new ConcurrentHashMap<>();
            this.inMemoryIdSequence = new AtomicLong(100);
        } else {
            this.inMemoryStore = null;
            this.inMemoryEmailIndex = null;
            this.inMemoryIdSequence = null;
        }
    }

    public UserAccount save(UserAccount user) {
        if (springDataUserRepository != null) {
            UserEntity entity = UserEntity.fromModel(user);
            UserEntity saved = springDataUserRepository.save(entity);
            return saved.toModel();
        }
        Long id = user.id();
        if (id == null) {
            id = inMemoryIdSequence.incrementAndGet();
            user = new UserAccount(id, user.name(), user.email(), user.passwordHash(), user.role(), user.createdAt());
        }
        inMemoryStore.put(id, user);
        inMemoryEmailIndex.put(user.email().toLowerCase().trim(), id);
        return user;
    }

    public Optional<UserAccount> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        if (springDataUserRepository != null) {
            return springDataUserRepository.findById(id).map(UserEntity::toModel);
        }
        return Optional.ofNullable(inMemoryStore.get(id));
    }

    public Optional<UserAccount> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String normalized = email.toLowerCase().trim();
        if (springDataUserRepository != null) {
            return springDataUserRepository.findByEmailIgnoreCase(normalized).map(UserEntity::toModel);
        }
        Long id = inMemoryEmailIndex.get(normalized);
        return id != null ? Optional.ofNullable(inMemoryStore.get(id)) : Optional.empty();
    }

    public boolean existsByEmail(String email) {
        if (email == null) {
            return false;
        }
        String normalized = email.toLowerCase().trim();
        if (springDataUserRepository != null) {
            return springDataUserRepository.existsByEmailIgnoreCase(normalized);
        }
        return inMemoryEmailIndex.containsKey(normalized);
    }

    public void clear() {
        if (springDataUserRepository != null) {
            springDataUserRepository.deleteAll();
        } else {
            inMemoryStore.clear();
            inMemoryEmailIndex.clear();
            inMemoryIdSequence.set(100);
        }
    }

    public long count() {
        if (springDataUserRepository != null) {
            return springDataUserRepository.count();
        }
        return inMemoryStore.size();
    }
}
