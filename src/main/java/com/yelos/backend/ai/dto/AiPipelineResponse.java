package com.yelos.backend.ai.dto;

import java.math.BigDecimal;

public record AiPipelineResponse(
        PiiFile piiFile,
        String previewCsv,
        Boolean flagged,
        String flagDetails,
        String summary,
        float[] embedding,
        DatasetRating rating
) {
        public record PiiFile(
                String fileName,
                String contentType,
                String contentBase64
        ) {
        }

        public record DatasetRating(
                BigDecimal completeness,
                BigDecimal consistency,
                BigDecimal uniqueness,
                BigDecimal validity,
                BigDecimal richness,
                BigDecimal total
        ) {
        }
}