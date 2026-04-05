package com.crm.dto;

import com.crm.entity.enums.PropertyStatus;
import com.crm.entity.enums.PropertyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PropertyRequest(
        @NotBlank String title,
        @NotBlank String location,
        @NotBlank String city,
        @NotBlank String locality,
        @NotNull PropertyType propertyType,
        @NotBlank String configuration,
        @NotNull @Min(100) Integer areaSqFt,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotNull PropertyStatus status,
        boolean featured,
        Long agentId
) {
}
