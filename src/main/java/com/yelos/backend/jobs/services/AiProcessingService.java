package com.yelos.backend.jobs.services;

import com.yelos.backend.ai.dto.AiPipelineResponse;
import com.yelos.backend.ai.services.AiService;
import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.repo.DataPostRepo;
import com.yelos.backend.jobs.dto.AiProcessingMessage;
import com.yelos.backend.storage.dto.LocalMultipartFile;
import com.yelos.backend.storage.dto.StoredFile;
import com.yelos.backend.storage.services.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;

@Service
public class AiProcessingService {

    private final DataPostRepo dataPostRepo;
    private final StorageService storageService;
    private final AiService aiService;

    public AiProcessingService(
            DataPostRepo dataPostRepo,
            StorageService storageService,
            AiService aiService
    ) {
        this.dataPostRepo = dataPostRepo;
        this.storageService = storageService;
        this.aiService = aiService;
    }

    public void process(AiProcessingMessage message) {
        DataPost post = dataPostRepo.findById(message.postId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Post not found"
                ));

        Path tempFile = null;

        try {
            post.markAiProcessing(Instant.now());
            dataPostRepo.save(post);

            tempFile = storageService.downloadToTempFile(
                    message.bucketName(),
                    message.objectKey(),
                    post.getFileName()
            );

            MultipartFile localFile = new LocalMultipartFile(
                    tempFile,
                    post.getFileName(),
                    post.getFileType()
            );

            AiPipelineResponse aiResult = aiService.runPipeline(localFile);

            if (aiResult == null || aiResult.piiFile() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI service did not return a PII file"
                );
            }

            AiPipelineResponse.PiiFile piiFile = aiResult.piiFile();

            if (piiFile.contentBase64() == null || piiFile.contentBase64().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI service returned empty PII file content"
                );
            }

            byte[] anonymizedFileBytes;

            try {
                anonymizedFileBytes = Base64.getDecoder().decode(piiFile.contentBase64());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI service returned invalid base64 file content"
                );
            }

            String processedFileName = piiFile.fileName() == null || piiFile.fileName().isBlank()
                    ? post.getFileName()
                    : piiFile.fileName();

            String processedContentType = piiFile.contentType() == null || piiFile.contentType().isBlank()
                    ? post.getFileType()
                    : piiFile.contentType();

            StoredFile overwrittenFile = storageService.overwriteDatasetBytes(
                    message.bucketName(),
                    message.objectKey(),
                    processedFileName,
                    processedContentType,
                    anonymizedFileBytes
            );

            String aiSummary = getAiSummaryOrFallback(aiResult);

            post.completeAiProcessingWithStoredFile(
                    aiSummary,
                    aiResult.previewCsv(),
                    aiResult.flagged(),
                    aiResult.flagDetails(),
                    overwrittenFile,
                    Instant.now()
            );

            dataPostRepo.save(post);

            System.out.println("AI processing completed for postId = " + post.getId());

        } catch (Exception e) {
            post.markAiFailed(getErrorMessage(e), Instant.now());
            dataPostRepo.save(post);

            System.out.println("AI processing failed for postId = " + post.getId());
            System.out.println("Reason = " + getErrorMessage(e));

            throw new RuntimeException("AI processing failed", e);

        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception deleteError) {
                    System.out.println("Failed to delete temp file: " + deleteError.getMessage());
                }
            }
        }
    }

    public void markFailed(Long postId, Exception e) {
        DataPost post = dataPostRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Post not found"
                ));

        post.markAiFailed(getErrorMessage(e), Instant.now());
        dataPostRepo.save(post);
    }

    private String getAiSummaryOrFallback(AiPipelineResponse aiResult) {
        if (aiResult != null && aiResult.summary() != null && !aiResult.summary().isBlank()) {
            return aiResult.summary().trim();
        }

        return "AI summary unavailable.";
    }

    private String getErrorMessage(Exception e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return "AI processing failed due to an unknown error";
        }

        return e.getMessage();
    }
}