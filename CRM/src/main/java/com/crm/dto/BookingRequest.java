package com.crm.dto;

import com.crm.entity.enums.BookingStatus;
import com.crm.entity.enums.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingRequest(
        @NotNull Long propertyId,
        @NotNull Long clientId,
        @NotNull Long agentId,
        @NotNull LocalDate bookingDate,
        @NotNull BookingStatus status,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal bookingAmount,
        BigDecimal amountPaid,
        PaymentStatus paymentStatus,
        LocalDate paymentDate,
        String paymentReference
) {
}
