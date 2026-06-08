package com.yelos.backend.storage.services;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Service
public class StorageDownloadService {

    private final S3Presigner s3Presigner;

    public StorageDownloadService(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

    public String generateDownloadUrl(
            String bucketName,
            String objectKey,
            String fileName
    ) {
        String safeFileName = fileName == null || fileName.isBlank()
                ? "dataset"
                : fileName.replace("\"", "");

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .responseContentDisposition("attachment; filename=\"" + safeFileName + "\"")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }
}