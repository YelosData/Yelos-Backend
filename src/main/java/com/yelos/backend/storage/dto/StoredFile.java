package com.yelos.backend.storage.dto;

import java.time.Instant;

public record StoredFile(
        String bucketName,
        String objectKey,
        String fileName,
        String fileType,
        Long fileSize,
        Instant uploadedAt
) {
}