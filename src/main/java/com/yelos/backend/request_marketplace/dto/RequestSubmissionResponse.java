package com.yelos.backend.request_marketplace.dto;

import com.yelos.backend.request_marketplace.models.SubmissionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RequestSubmissionResponse(
        Long id,
        Long requestId,
        Long sellerId,
        String sellerUsername,
        String message,
        BigDecimal proposedPrice,
        SubmissionStatus status,
        String fileName,
        String fileType,
        Long fileSize,
        Instant submittedAt,
        Instant acceptedAt,
        Instant rejectedAt
) {}