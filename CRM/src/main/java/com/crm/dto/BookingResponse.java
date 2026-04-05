package com.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        LocalDate bookingDate,
        String status,
        BigDecimal bookingAmount,
        BigDecimal amountPaid,
        BigDecimal balanceAmount,
        String paymentStatus,
        LocalDate paymentDate,
        String paymentReference,
        LocalDateTime createdAt,
        PropertySummaryResponse property,
        ClientSummaryResponse client,
        AgentSummaryResponse agent
) {
}
