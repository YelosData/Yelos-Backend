package com.yelos.backend.datapost.dto;

import java.math.BigDecimal;

public record DataQualityRatingResponse(
        BigDecimal completeness,
        BigDecimal consistency,
        BigDecimal uniqueness,
        BigDecimal validity,
        BigDecimal richness,
        BigDecimal total
) {
}