package com.ridehailing.auth.dto;

import com.ridehailing.auth.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Name must not be blank")
    String name,

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be well-formed")
    String email,

    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password,

    UserRole role
) {
    public RegisterRequest(String name, String email, String password) {
        this(name, email, password, null);
    }
}
