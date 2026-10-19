package com.ridehailing.driver.security;

public record AuthenticatedUser(
    Long userId,
    String email,
    String role
) {
}
