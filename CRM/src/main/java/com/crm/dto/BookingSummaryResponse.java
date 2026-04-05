package com.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingSummaryResponse(
        Long id,
        LocalDate bookingDate,
        String status,
        BigDecimal bookingAmount,
        BigDecimal amountPaid,
        BigDecimal balanceAmount,
        String paymentStatus,
        PropertySummaryResponse property,
        ClientSummaryResponse client,
        AgentSummaryResponse agent
) {
}
