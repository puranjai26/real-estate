package com.crm.dto;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String name,
        String email,
        String role,
        Long brokerId,
        Long agentId
) {
}
