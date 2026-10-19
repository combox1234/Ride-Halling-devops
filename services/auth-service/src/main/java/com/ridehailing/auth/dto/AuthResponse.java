package com.ridehailing.auth.dto;

import com.ridehailing.auth.model.UserRole;

public record AuthResponse(
    String token,
    String tokenType,
    Long userId,
    String email,
    UserRole role,
    long expiresIn
) {
    public static AuthResponse bearer(String token, Long userId, String email, UserRole role, long expiresIn) {
        return new AuthResponse(token, "Bearer", userId, email, role, expiresIn);
    }
}
