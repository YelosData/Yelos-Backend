package com.yelos.backend.ai.dto;

import com.yelos.backend.storage.dto.StoredFile;

public record ProcessedAiFile(
        StoredFile storedFile,
        AiPipelineResponse aiResult
) {
}