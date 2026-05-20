package com.yelos.backend.datapost.dto;

import com.yelos.backend.datapost.models.Genre;

import java.math.BigDecimal;
import java.time.Instant;

public record DataPostResponse(
        long id,
        long userId,
        String username,
        String title,
        String summary,
        String content,
        Genre genre,
        BigDecimal price,
        Instant createdAt
) {
}
