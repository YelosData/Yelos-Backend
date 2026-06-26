package com.yelos.backend.storage.services;

import com.yelos.backend.storage.dto.StoredFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class StorageService {

    private final S3Client s3Client;

    @Value("${supabase.storage.bucket}")
    private String bucketName;

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10mb

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "csv",
            "json",
            "txt",
            "xlsx"
    );

    public StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public StoredFile uploadDatasetFile(Long sellerId, MultipartFile file) {
        validateFile(file);

        String safeFileName = getSafeFileName(file);
        String fileType = getFileType(file);
        String objectKey = buildObjectKey(sellerId, safeFileName);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(fileType)
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return new StoredFile(
                    bucketName,
                    objectKey,
                    safeFileName,
                    fileType,
                    file.getSize(),
                    Instant.now()
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read uploaded file"
            );
        } catch (SdkException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to upload file to storage"
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dataset file is required"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dataset file must be smaller than 10MB"
            );
        }

        String extension = getFileExtension(file);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only CSV, JSON, TXT, or XLSX files are allowed"
            );
        }
    }

    public StoredFile uploadDatasetBytes(
            Long sellerId,
            String fileName,
            String fileType,
            byte[] bytes
    ) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dataset file is required"
            );
        }

        if (bytes.length > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dataset file must be smaller than 10MB"
            );
        }

        String safeFileName = getSafeFileName(fileName);
        String extension = getFileExtension(safeFileName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only CSV, JSON, TXT, or XLSX files are allowed"
            );
        }

        String safeFileType = fileType == null || fileType.isBlank()
                ? "text/csv"
                : fileType;

        String objectKey = buildObjectKey(sellerId, safeFileName);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(safeFileType)
                    .contentLength((long) bytes.length)
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(bytes)
            );

            return new StoredFile(
                    bucketName,
                    objectKey,
                    safeFileName,
                    safeFileType,
                    (long) bytes.length,
                    Instant.now()
            );

        } catch (SdkException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to upload file to storage"
            );
        }
    }

    private String getSafeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "dataset.csv";
        }

        return fileName
                .trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getSafeFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            return "dataset";
        }

        return originalFileName
                .trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");

        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String getFileExtension(MultipartFile file) {
        String fileName = getSafeFileName(file);
        int lastDotIndex = fileName.lastIndexOf(".");

        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String getFileType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }

        return contentType;
    }

    private String buildObjectKey(Long sellerId, String safeFileName) {
        return "sellers/%d/posts/%s-%s"
                .formatted(sellerId, UUID.randomUUID(), safeFileName);
    }
}