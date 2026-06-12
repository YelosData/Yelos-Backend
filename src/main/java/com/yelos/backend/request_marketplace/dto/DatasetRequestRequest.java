package com.yelos.backend.request_marketplace.dto;

import com.yelos.backend.datapost.models.Genre;

import java.math.BigDecimal;

public record DatasetRequestRequest(
        Long buyerId,
        Genre genre,
        String specification,
        BigDecimal budget
) {
}
