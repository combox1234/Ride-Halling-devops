package com.ridehailing.gateway.security;

public record JwtValidationResult(
    boolean isValid,
    AuthenticatedUser user,
    String errorMessage
) {
    public static JwtValidationResult valid(AuthenticatedUser user) {
        return new JwtValidationResult(true, user, null);
    }

    public static JwtValidationResult invalid(String errorMessage) {
        return new JwtValidationResult(false, null, errorMessage);
    }
}
