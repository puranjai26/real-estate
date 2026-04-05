package com.crm.dto;

import com.crm.entity.enums.InterestType;
import com.crm.entity.enums.LeadSource;
import com.crm.entity.enums.LeadStage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeadRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String phone,
        @NotBlank String preferredLocation,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal budgetMin,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal budgetMax,
        @NotNull InterestType interestType,
        @NotNull LeadSource source,
        LocalDate followUpDate,
        @NotNull LeadStage stage,
        String notes,
        Long agentId
) {
}
