package com.yelos.backend.request_marketplace.dto;

import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.request_marketplace.models.RequestStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record DatasetRequestResponse(
        Long id,
        Long buyerId,
        String buyerUsername,
        Genre genre,
        String specification,
        BigDecimal budget,
        RequestStatus status,
        Long acceptedSubmissionId,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {}