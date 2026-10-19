package com.ridehailing.driver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDriverRequest(
    @NotNull(message = "userId is required")
    Long userId,

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    String name,

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    @Size(max = 255, message = "email must not exceed 255 characters")
    String email,

    @NotBlank(message = "phone is required")
    @Size(max = 30, message = "phone must not exceed 30 characters")
    String phone,

    @NotBlank(message = "vehicleMake is required")
    @Size(max = 50, message = "vehicleMake must not exceed 50 characters")
    String vehicleMake,

    @NotBlank(message = "vehicleModel is required")
    @Size(max = 50, message = "vehicleModel must not exceed 50 characters")
    String vehicleModel,

    @NotBlank(message = "licensePlate is required")
    @Size(max = 20, message = "licensePlate must not exceed 20 characters")
    String licensePlate
) {
}
