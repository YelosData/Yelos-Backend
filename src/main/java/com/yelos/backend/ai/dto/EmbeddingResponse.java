package com.yelos.backend.ai.dto;

public record EmbeddingResponse(
        int dimensions,
        float[] embedding
) {
}
