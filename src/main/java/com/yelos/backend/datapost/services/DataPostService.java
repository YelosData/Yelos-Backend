package com.yelos.backend.datapost.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.yelos.backend.ai.dto.AiPipelineResponse;
import com.yelos.backend.ai.dto.ProcessedAiFile;
import com.yelos.backend.ai.services.AiService;
import com.yelos.backend.datapost.dto.DataPostPreviewResponse;
import com.yelos.backend.datapost.dto.DataPostResponse;
import com.yelos.backend.datapost.dto.DataQualityRatingResponse;
import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.repo.DataPostRepo;
import com.yelos.backend.globaldto.PageResponse;
import com.yelos.backend.jobs.AiProcessingProducer;
import com.yelos.backend.jobs.dto.AiProcessingMessage;
import com.yelos.backend.storage.dto.StoredFile;
import com.yelos.backend.storage.services.StorageService;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.repo.UserRepo;


@Service
public class DataPostService {

    private final DataPostRepo dataPostRepo;
    private final UserRepo userRepo;
    private final StorageService storageService;
    private final AiService aiService;
    private final AiProcessingProducer aiProcessingProducer;

    public DataPostService(
            DataPostRepo dataPostRepo,
            UserRepo userRepo,
            StorageService storageService,
            AiService aiService,
            AiProcessingProducer aiProcessingProducer
    ) {
        this.dataPostRepo = dataPostRepo;
        this.userRepo = userRepo;
        this.storageService = storageService;
        this.aiService = aiService;
        this.aiProcessingProducer = aiProcessingProducer;
    }


    public DataPostResponse postDataPosts(
            Long userId,
            String title,
            BigDecimal price,
            Genre genre,
            MultipartFile file
    ) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User does not exist"
                ));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dataset file is required"
            );
        }

        StoredFile storedFile;

        try {
            /*
             * Upload the original file only.
             * The RabbitMQ worker will run the AI pipeline asynchronously.
             */
            storedFile = storageService.uploadDatasetFile(
                    userId,
                    file
            );
        } catch (Exception e) {
            e.printStackTrace();

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to upload dataset file"
            );
        }

        String pendingSummary =
                "AI processing is pending. Please check back shortly.";

        DataPost post = new DataPost(
                user,
                genre,
                title.trim(),
                pendingSummary,
                price,
                DataPostStatus.PENDING,
                Instant.now(),
                storedFile.fileName(),
                storedFile.fileType(),
                storedFile.fileSize(),
                storedFile.bucketName(),
                storedFile.objectKey()
        );

        DataPost savedPost = dataPostRepo.save(post);

        aiProcessingProducer.send(
                new AiProcessingMessage(
                        savedPost.getId(),
                        userId,
                        savedPost.getBucketName(),
                        savedPost.getObjectKey()
                )
        );

        /*
         * Return immediately after saving and queueing.
         * The frontend can now redirect to My Posts.
         */
        return toDataPostResponse(savedPost);
    }


    public DataPostResponse getDataPostById(long id) {
        DataPost post = dataPostRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Data post not found"
                ));

        return toDataPostResponse(post);
    }


    public PageResponse<DataPostPreviewResponse> getDataPost(
            int page,
            int size,
            Genre genre,
            String sort
    ) {
        if (page < 0) {
            page = 0;
        }

        if (size < 1 || size > 50) {
            size = 12;
        }

        Sort sortObj = switch (sort) {
            case "priceAsc" ->
                    Sort.by("price").ascending();

            case "priceDesc" ->
                    Sort.by("price").descending();

            case "oldest" ->
                    Sort.by("createdAt").ascending();

            default ->
                    Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(
                page,
                size,
                sortObj
        );

        Page<DataPost> result = genre == null
                ? dataPostRepo.findByStatus(
                DataPostStatus.ACTIVE,
                pageable
        )
                : dataPostRepo.findByGenreAndStatus(
                genre,
                DataPostStatus.ACTIVE,
                pageable
        );

        List<DataPostPreviewResponse> content =
                result.getContent()
                        .stream()
                        .map(this::toPreviewResponse)
                        .toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }


    public List<DataPostPreviewResponse> searchDataPosts(
            String query,
            int limit
    ) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        if (limit < 1) {
            limit = 10;
        }

        float[] queryEmbedding =
                aiService.embedQuery(query.trim());

        List<DataPost> activePosts =
                dataPostRepo.findByStatus(
                        DataPostStatus.ACTIVE
                );

        record ScoredPost(
                DataPost post,
                double score
        ) {
        }

        List<ScoredPost> scoredPosts = activePosts.stream()
                .filter(post -> post.getEmbedding() != null)
                .map(post -> new ScoredPost(
                        post,
                        cosineSimilarity(
                                queryEmbedding,
                                post.getEmbedding()
                        )
                ))
                .filter(scoredPost ->
                        scoredPost.score() > 0.25
                )
                .sorted(
                        Comparator.comparingDouble(
                                ScoredPost::score
                        ).reversed()
                )
                .limit(limit)
                .toList();

        return scoredPosts.stream()
                .map(scoredPost ->
                        toPreviewResponse(scoredPost.post())
                )
                .toList();
    }


    public PageResponse<DataPostResponse> getAllDataPostsByUid(
            Long userId,
            int page,
            int size,
            String sort
    ) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User does not exist"
                ));

        if (page < 0) {
            page = 0;
        }

        if (size < 1 || size > 50) {
            size = 12;
        }

        Sort sortObj = switch (sort) {
            case "oldest" ->
                    Sort.by("createdAt").ascending();

            case "priceAsc" ->
                    Sort.by("price").ascending();

            case "priceDesc" ->
                    Sort.by("price").descending();

            default ->
                    Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(
                page,
                size,
                sortObj
        );

        Page<DataPost> result =
                dataPostRepo.findByUserIdAndStatusNot(
                        user.getId(),
                        DataPostStatus.DELETED,
                        pageable
                );

        List<DataPostResponse> content =
                result.getContent()
                        .stream()
                        .map(this::toDataPostResponse)
                        .toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }


    @Transactional
    public void deletePost(
            Long postId,
            Long userId
    ) {
        DataPost post = dataPostRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Post does not exist"
                ));

        if (!Objects.equals(
                post.getUser().getId(),
                userId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only delete your own posts"
            );
        }

        if (post.isDeleted()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Post is already deleted"
            );
        }

        post.markDeleted(Instant.now());
    }


    @Transactional
    public DataPostResponse updatePost(
            Long postId,
            Long userId,
            String title,
            String summary,
            Genre genre,
            BigDecimal price,
            MultipartFile file
    ) {
        DataPost post = dataPostRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Post does not exist"
                ));

        if (!Objects.equals(
                post.getUser().getId(),
                userId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only update your own posts"
            );
        }

        if (post.isDeleted()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot update a deleted post"
            );
        }

        validateUpdatePost(
                title,
                summary,
                genre,
                price
        );

        StoredFile storedFile = null;
        String updatedSummary = summary.trim();

        if (file != null && !file.isEmpty()) {
            /*
             * File replacement remains synchronous for now.
             * Initial post creation is handled through RabbitMQ.
             */
            ProcessedAiFile processedAiFile =
                    processFileWithAiAndUpload(
                            userId,
                            file
                    );

            storedFile = processedAiFile.storedFile();

            AiPipelineResponse aiResult =
                    processedAiFile.aiResult();

            updatedSummary =
                    getAiSummaryOrFallback(aiResult);

            post.attachAiMetadata(
                    updatedSummary,
                    aiResult.previewCsv(),
                    aiResult.flagged(),
                    aiResult.flagDetails()
            );

            post.setEmbedding(
                    aiResult.embedding()
            );

            attachQualityRating(
                    post,
                    aiResult
            );
        }

        post.updatePost(
                genre,
                title.trim(),
                updatedSummary,
                price,
                storedFile,
                Instant.now()
        );

        return toDataPostResponse(post);
    }


    @Transactional(readOnly = true)
    public PageResponse<DataPostResponse> getUnreviewedActivePosts(
            int page,
            int size,
            String sort
    ) {
        Sort sortObj = switch (sort) {
            case "priceAsc" ->
                    Sort.by("price").ascending();

            case "priceDesc" ->
                    Sort.by("price").descending();

            case "oldest" ->
                    Sort.by("createdAt").ascending();

            default ->
                    Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(
                page,
                size,
                sortObj
        );

        Page<DataPost> result =
                dataPostRepo.findByStatusAndAdminReviewedFalse(
                        DataPostStatus.ACTIVE,
                        pageable
                );

        return new PageResponse<>(
                result.getContent()
                        .stream()
                        .map(this::toDataPostResponse)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }


    @Transactional
    public DataPostResponse reviewPost(
            Long postId,
            Long adminId,
            DataPostStatus status,
            String note
    ) {
        DataPost post = dataPostRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Post does not exist"
                ));

        if (adminId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin ID is required"
            );
        }

        if (
                status != DataPostStatus.ACTIVE
                        && status != DataPostStatus.DELETED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin review status must be ACTIVE or DELETED"
            );
        }

        String cleanedNote =
                note == null
                        ? null
                        : note.trim();

        Instant now = Instant.now();

        if (status == DataPostStatus.ACTIVE) {
            post.markReviewed(
                    adminId,
                    cleanedNote,
                    now
            );
        } else {
            post.hideByAdmin(
                    adminId,
                    cleanedNote,
                    now
            );
        }

        return toDataPostResponse(post);
    }


    private void validateUpdatePost(
            String title,
            String summary,
            Genre genre,
            BigDecimal price
    ) {
        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Title is required"
            );
        }

        if (summary == null || summary.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Summary is required"
            );
        }

        if (genre == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Genre is required"
            );
        }

        if (
                price == null
                        || price.compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Price must be greater than 0"
            );
        }
    }


    private ProcessedAiFile processFileWithAiAndUpload(
            Long userId,
            MultipartFile file
    ) {
        AiPipelineResponse aiResult =
                aiService.runPipeline(file);

        if (
                aiResult == null
                        || aiResult.piiFile() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI service did not return a PII file"
            );
        }

        AiPipelineResponse.PiiFile piiFile =
                aiResult.piiFile();

        if (
                piiFile.contentBase64() == null
                        || piiFile.contentBase64().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI service returned empty PII file content"
            );
        }

        byte[] anonymizedCsvBytes;

        try {
            anonymizedCsvBytes =
                    Base64.getDecoder()
                            .decode(
                                    piiFile.contentBase64()
                            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI service returned invalid base64 CSV content"
            );
        }

        StoredFile storedFile =
                storageService.uploadDatasetBytes(
                        userId,
                        piiFile.fileName(),
                        piiFile.contentType(),
                        anonymizedCsvBytes
                );

        return new ProcessedAiFile(
                storedFile,
                aiResult
        );
    }


    private double cosineSimilarity(
            float[] a,
            float[] b
    ) {
        if (
                a == null
                        || b == null
                        || a.length == 0
                        || a.length != b.length
        ) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct
                / (
                Math.sqrt(normA)
                        * Math.sqrt(normB)
        );
    }


    private String getAiSummaryOrFallback(
            AiPipelineResponse aiResult
    ) {
        if (
                aiResult != null
                        && aiResult.summary() != null
                        && !aiResult.summary().isBlank()
        ) {
            return aiResult.summary().trim();
        }

        return "AI summary unavailable.";
    }


    private void attachQualityRating(
            DataPost post,
            AiPipelineResponse aiResult
    ) {
        if (
                aiResult == null
                        || aiResult.rating() == null
        ) {
            return;
        }

        AiPipelineResponse.DatasetRating rating =
                aiResult.rating();

        post.attachQualityRating(
                rating.completeness(),
                rating.consistency(),
                rating.uniqueness(),
                rating.validity(),
                rating.richness(),
                rating.total()
        );
    }


    private DataQualityRatingResponse toQualityRatingResponse(
            DataPost post
    ) {
        boolean hasNoRating =
                post.getQualityCompleteness() == null
                        && post.getQualityConsistency() == null
                        && post.getQualityUniqueness() == null
                        && post.getQualityValidity() == null
                        && post.getQualityRichness() == null
                        && post.getQualityTotal() == null;

        if (hasNoRating) {
            return null;
        }

        return new DataQualityRatingResponse(
                post.getQualityCompleteness(),
                post.getQualityConsistency(),
                post.getQualityUniqueness(),
                post.getQualityValidity(),
                post.getQualityRichness(),
                post.getQualityTotal()
        );
    }


    private DataPostPreviewResponse toPreviewResponse(
            DataPost post
    ) {
        return new DataPostPreviewResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getTitle(),
                post.getSummary(),
                post.getGenre(),
                post.getPrice(),
                post.getCreatedAt(),
                post.getAdminReviewed(),
                toQualityRatingResponse(post)
        );
    }


    private DataPostResponse toDataPostResponse(
            DataPost post
    ) {
        return new DataPostResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getTitle(),
                post.getSummary(),
                post.getGenre(),
                post.getPrice(),
                post.getCreatedAt(),
                post.getFileName(),
                post.getFileType(),
                post.getFileSize(),
                post.getUpdatedAt(),
                post.getStatus(),
                post.getVersion(),
                post.getAdminReviewed(),
                post.getReviewedAt(),
                post.getReviewedByAdminId(),
                post.getAdminNote(),
                post.getPreviewCsv(),
                post.getAiFlagged(),
                post.getAiFlagDetails(),
                toQualityRatingResponse(post)
        );
    }
}
