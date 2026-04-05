package com.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PropertyResponse(
        Long id,
        String title,
        String location,
        String city,
        String locality,
        String propertyType,
        String configuration,
        Integer areaSqFt,
        BigDecimal price,
        String status,
        boolean featured,
        LocalDateTime createdAt,
        AgentSummaryResponse agent,
        long bookingCount,
        List<BookingSummaryResponse> bookings
) {
}
