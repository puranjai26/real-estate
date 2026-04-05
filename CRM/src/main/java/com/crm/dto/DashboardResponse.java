package com.crm.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long totalLeads,
        long totalProperties,
        long totalClients,
        long totalBookings,
        long availableProperties,
        long soldProperties,
        long pendingBookings,
        long confirmedBookings,
        long cancelledBookings,
        BigDecimal totalPortfolioValue,
        BigDecimal availableInventoryValue,
        BigDecimal soldInventoryValue,
        BigDecimal averagePropertyPrice,
        BigDecimal totalBookingValue,
        BigDecimal collectedPayments,
        BigDecimal outstandingPayments,
        double bookingConversionRate,
        List<DashboardDistributionItem> propertyTypeMix,
        List<DashboardDistributionItem> cityMix,
        List<DashboardDistributionItem> bookingStatusMix,
        List<DashboardDistributionItem> leadStageMix,
        List<DashboardDistributionItem> leadSourceMix,
        List<DashboardBrokerSnapshot> topBrokers,
        List<DashboardUpcomingBooking> upcomingBookings,
        List<DashboardLeadFollowUp> upcomingLeadFollowUps
) {
}
