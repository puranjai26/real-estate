package com.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @Size(min = 6, message = "Password must be at least 6 characters") String password
) {
}
