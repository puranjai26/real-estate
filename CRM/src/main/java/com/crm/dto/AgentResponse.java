package com.crm.dto;

import java.util.List;

public record AgentResponse(
        Long id,
        String name,
        String email,
        long propertyCount,
        long bookingCount,
        long leadCount,
        List<PropertySummaryResponse> properties,
        List<BookingSummaryResponse> bookings
) {
}
