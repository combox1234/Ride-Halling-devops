package com.ridehailing.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be well-formed")
    String email,

    @NotBlank(message = "Password must not be blank")
    String password
) {
}
