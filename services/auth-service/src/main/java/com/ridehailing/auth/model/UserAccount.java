package com.ridehailing.auth.model;

import java.time.Instant;

public record UserAccount(
    Long id,
    String name,
    String email,
    String passwordHash,
    UserRole role,
    Instant createdAt
) {
}
