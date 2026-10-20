package com.ridehailing.gateway.security;

import java.util.Objects;

public record AuthenticatedUser(Long userId, String email, String role) {
    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(role, "role must not be null");
    }
}
