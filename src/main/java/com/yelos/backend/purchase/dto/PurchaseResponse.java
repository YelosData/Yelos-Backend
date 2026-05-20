package com.yelos.backend.purchase.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchaseResponse(
        Long id,
        Long buyerId,
        Long sellerId,
        Long postId,
        BigDecimal price,
        BigDecimal buyerCredits,
        String titleSnapshot,
        String summarySnapshot,
        String contentSnapshot,
        Instant purchasedAt
) {}
