package com.yelos.backend.request_marketplace.dto;

public record RequestSubmissionDownloadResponse(
        String downloadUrl,
        String fileName,
        String fileType,
        Long fileSize,
        int expiresInSeconds
) {}