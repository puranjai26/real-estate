package com.crm.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ClientResponse(
        Long id,
        String name,
        String email,
        String phone,
        String preferredLocation,
        LocalDateTime createdAt,
        long bookingCount,
        List<BookingSummaryResponse> bookings
) {
}
