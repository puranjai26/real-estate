package com.crm.dto;

public record DashboardBrokerSnapshot(
        String name,
        long propertyCount,
        long bookingCount,
        long confirmedDeals
) {
}
