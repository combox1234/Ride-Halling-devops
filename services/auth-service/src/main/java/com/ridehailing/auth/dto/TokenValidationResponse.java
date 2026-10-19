package com.ridehailing.auth.dto;

import com.ridehailing.auth.model.UserRole;

public record TokenValidationResponse(
    boolean valid,
    Long userId,
    String email,
    UserRole role
) {
    public static TokenValidationResponse valid(Long userId, String email, UserRole role) {
        return new TokenValidationResponse(true, userId, email, role);
    }

    public static TokenValidationResponse invalid() {
        return new TokenValidationResponse(false, null, null, null);
    }
}
