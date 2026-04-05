package com.crm.dto;

import java.math.BigDecimal;

public record PropertySummaryResponse(
        Long id,
        String title,
        String location,
        String city,
        String locality,
        String propertyType,
        String configuration,
        Integer areaSqFt,
        BigDecimal price,
        String status,
        boolean featured
) {
}
