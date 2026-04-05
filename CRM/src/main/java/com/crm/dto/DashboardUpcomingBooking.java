package com.crm.dto;

import java.time.LocalDate;

public record DashboardUpcomingBooking(
        String propertyTitle,
        String clientName,
        String location,
        LocalDate bookingDate,
        String status
) {
}
