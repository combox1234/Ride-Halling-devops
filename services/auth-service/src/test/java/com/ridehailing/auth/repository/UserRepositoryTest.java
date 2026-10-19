package com.ridehailing.auth.repository;

import com.ridehailing.auth.entity.UserEntity;
import com.ridehailing.auth.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserRepositoryTest {

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.clear();
    }

    @Test
    @DisplayName("Should persist and retrieve UserEntity via Spring Data JPA and Flyway schema")
    void shouldPersistAndFindUserEntity() {
        UserEntity entity = new UserEntity(
            null,
            "Alice Test",
            "alice.jpa@example.com",
            "$2a$12$hashedPasswordExample",
            UserRole.ROLE_PASSENGER,
            Instant.now()
        );

        UserEntity saved = springDataUserRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("alice.jpa@example.com");

        Optional<UserEntity> found = springDataUserRepository.findByEmailIgnoreCase("ALICE.JPA@EXAMPLE.COM");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice Test");
        assertThat(found.get().getRole()).isEqualTo(UserRole.ROLE_PASSENGER);
    }

    @Test
    @DisplayName("UserRepository should persist UserAccount to relational database")
    void shouldPersistUserAccountViaRepository() {
        com.ridehailing.auth.model.UserAccount account = new com.ridehailing.auth.model.UserAccount(
            null,
            "Bob Test",
            "bob.jpa@example.com",
            "hash123",
            UserRole.ROLE_DRIVER,
            Instant.now()
        );

        com.ridehailing.auth.model.UserAccount saved = userRepository.save(account);

        assertThat(saved.id()).isNotNull();
        assertThat(userRepository.existsByEmail("bob.jpa@example.com")).isTrue();
        assertThat(userRepository.findByEmail("BOB.JPA@EXAMPLE.COM")).isPresent();
        assertThat(userRepository.findById(saved.id())).isPresent();
    }
}
