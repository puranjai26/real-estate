package com.crm.dto;

import java.time.LocalDate;

public record DashboardLeadFollowUp(
        String leadName,
        String preferredLocation,
        String stage,
        String source,
        LocalDate followUpDate,
        String brokerName
) {
}
