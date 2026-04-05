package com.crm.dto;

public record ClientSummaryResponse(
        Long id,
        String name,
        String email,
        String phone,
        String preferredLocation
) {
}
