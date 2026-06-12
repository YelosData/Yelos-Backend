package com.yelos.backend.purchase.dto;

import com.yelos.backend.datapost.models.Genre;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchaseResponse(
        Long id,
        Long buyerId,
        Long sellerId,
        Long postId,
        BigDecimal price,
        BigDecimal buyerCredits,
        Genre genre,
        String titleSnapshot,
        String summarySnapshot,
        String fileNameSnapshot,
        String fileTypeSnapshot,
        Long fileSizeSnapshot,
        Instant purchasedAt
) {}
