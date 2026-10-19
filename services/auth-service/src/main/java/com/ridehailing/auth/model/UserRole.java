package com.ridehailing.auth.model;

public enum UserRole {
    ROLE_PASSENGER,
    ROLE_DRIVER,
    ROLE_ADMIN;

    public boolean isPubliclyRegistrable() {
        return this == ROLE_PASSENGER || this == ROLE_DRIVER;
    }
}
