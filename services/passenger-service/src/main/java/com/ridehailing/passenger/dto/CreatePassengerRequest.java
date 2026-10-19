package com.ridehailing.passenger.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePassengerRequest(
    @NotNull(message = "User ID must not be null")
    Long userId,

    @NotBlank(message = "Name must not be blank")
    String name,

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be well-formed")
    String email,

    @NotBlank(message = "Phone number must not be blank")
    String phone
) {
}
