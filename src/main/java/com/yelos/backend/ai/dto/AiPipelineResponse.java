package com.yelos.backend.ai.dto;

public record AiPipelineResponse(
        PiiFile piiFile,
        String previewCsv,
        Boolean flagged,
        String flagDetails,
        String summary
) {
        public record PiiFile(
                String fileName,
                String contentType,
                String contentBase64
        ) {
        }
}