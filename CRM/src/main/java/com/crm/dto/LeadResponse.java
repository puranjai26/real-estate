package com.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeadResponse(
        Long id,
        String name,
        String email,
        String phone,
        String preferredLocation,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        String interestType,
        String source,
        LocalDate followUpDate,
        String stage,
        String notes,
        LocalDateTime createdAt,
        AgentSummaryResponse agent
) {
}
