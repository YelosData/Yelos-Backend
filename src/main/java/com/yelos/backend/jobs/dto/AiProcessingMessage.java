package com.yelos.backend.jobs.dto;

public record AiProcessingMessage(
        Long postId,
        Long userId,
        String bucketName,
        String objectKey
) {
}