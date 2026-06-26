package com.yelos.backend.ai.controllers;

import com.yelos.backend.ai.dto.AiPipelineResponse;
import com.yelos.backend.ai.services.AiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/test")
public class AiTestController {

    private final AiService aiService;

    public AiTestController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping(
            value = "/run-pipeline",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AiPipelineResponse runPipeline(
            @RequestParam("file") MultipartFile file
    ) {
        return aiService.runPipeline(file);
    }
}