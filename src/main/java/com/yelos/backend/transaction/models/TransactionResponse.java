package com.yelos.backend.transaction.models;

import com.yelos.backend.transaction.models.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        Long userId,
        Long purchaseId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant createdAt
) {
}