package com.yelos.backend.datapost.dto;

import com.yelos.backend.datapost.models.Genre;

import java.math.BigDecimal;

public record PostRequest(
        Long userId,
        String title,
        String content,
        String summary,
        BigDecimal price,
        Genre genre
) {
}
