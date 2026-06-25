package com.yelos.backend.datapost.dto;

import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;

import java.math.BigDecimal;
import java.time.Instant;

public record DataPostResponse(
        Long id,
        Long userId,
        String username,
        String title,
        String summary,
        Genre genre,
        BigDecimal price,
        Instant createdAt,
        String fileName,
        String fileType,
        Long fileSize,
        Instant updatedAt,
        DataPostStatus status,
        Integer version,
        Boolean adminReviewed,
        Instant reviewedAt,
        Long reviewedByAdminId,
        String adminNote,

        String previewCsv,
        Boolean aiFlagged,
        String aiFlagDetails
) {
}
