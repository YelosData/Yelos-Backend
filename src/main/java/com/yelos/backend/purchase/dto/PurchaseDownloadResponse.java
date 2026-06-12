package com.yelos.backend.purchase.dto;

public record PurchaseDownloadResponse(
        String downloadUrl,
        String fileName,
        String fileType,
        Long fileSize,
        Integer expiresInSeconds
) {
}