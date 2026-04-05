package com.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String phone,
        String preferredLocation
) {
}
