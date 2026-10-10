package com.fioneer.insurance.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 2, max = 50)
        String firstName,

        @NotBlank
        @Size(min = 2, max = 50)
        String lastName,

        @NotBlank
        @Size(min = 4, max = 50)
        String username,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotBlank
        @Email
        @Size(max = 100)
        String email,

        @Size(max = 50)
        String nationalId,

        @Size(min = 2, max = 100)
        String street,

        @Size(max = 10)
        String streetNumber,

        @Size(max = 10)
        String zipCode,

        @Size(min = 2, max = 50)
        String city
) {
}
