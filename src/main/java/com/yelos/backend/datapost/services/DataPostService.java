package com.yelos.backend.datapost.services;


import com.yelos.backend.datapost.dto.DataPostPreviewResponse;
import com.yelos.backend.datapost.dto.PostRequest;
import com.yelos.backend.datapost.dto.DataPostResponse;
import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.repo.DataPostRepo;
import com.yelos.backend.globaldto.PageResponse;
import com.yelos.backend.storage.dto.StoredFile;
import com.yelos.backend.storage.services.StorageService;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.repo.UserRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


@Service
public class DataPostService {
    private final DataPostRepo dataPostRepo;
    private final UserRepo userRepo;
    private final StorageService storageService;

    public DataPostService(UserRepo userRepo, DataPostRepo dataPostRepo, StorageService storageService){
        this.userRepo = userRepo;
        this.dataPostRepo = dataPostRepo;
        this.storageService = storageService;
    }


    public DataPostResponse postDataPosts(
            Long userId,
            String title,
            String summary,
            BigDecimal price,
            Genre genre,
            MultipartFile file
    ) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        StoredFile storedFile = null;

        try {
            storedFile = storageService.uploadDatasetFile(userId, file);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        DataPost post = new DataPost(
                user,
                genre,
                title,
                summary,
                price,
                DataPostStatus.ACTIVE,
                Instant.now(),
                storedFile.fileName(),
                storedFile.fileType(),
                storedFile.fileSize(),
                storedFile.bucketName(),
                storedFile.objectKey()
        );

        DataPost savedPost = dataPostRepo.save(post);

        return toDataPostResponse(savedPost);
    }

    // Not in use for now
//    public List<GenreReponse> getAllPostGenres() {
//        return dataPostRepo.findDistinctGenre().stream()
//                .map(GenreReponse::new)
//                .toList();
//    }

    public DataPostResponse getDataPostById(long id) {
        DataPost post = dataPostRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Data post not found"));

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
            case "priceAsc" -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<DataPost> result = genre == null
                ? dataPostRepo.findByStatus(DataPostStatus.ACTIVE, pageable)
                : dataPostRepo.findByGenreAndStatus(
                genre,
                DataPostStatus.ACTIVE,
                pageable
        );

        List<DataPostPreviewResponse> content = result.getContent()
                .stream()
                .map(this::toPreviewResponse).toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    public PageResponse<DataPostResponse> getAllDataPostsByUid(
            Long userId,
            int page,
            int size,
            String sort
    ) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        if (page < 0) {
            page = 0;
        }

        if (size < 1 || size > 50) {
            size = 12;
        }

        Sort sortObj = switch (sort) {
            case "oldest" -> Sort.by("createdAt").ascending();
            case "priceAsc" -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            default -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<DataPost> result = dataPostRepo.findByUserIdAndStatusNot(
                user.getId(),
                DataPostStatus.DELETED,
                pageable
        );

        List<DataPostResponse> content = result.getContent()
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
    public void deletePost(Long postId, Long userId) {
        DataPost post = dataPostRepo.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Post does not exist"
                ));

        if (post.getUser().getId() != userId) {
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

        if (post.getUser().getId() != userId) {
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

        validateUpdatePost(title, summary, genre, price);

        StoredFile storedFile = null;

        if (file != null && !file.isEmpty()) {
            storedFile = storageService.uploadDatasetFile(userId, file);
        }

        post.updatePost(
                genre,
                title.trim(),
                summary.trim(),
                price,
                storedFile,
                Instant.now()
        );

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

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Price must be greater than 0"
            );
        }
    }

    private DataPostPreviewResponse toPreviewResponse(DataPost post) {
        return new DataPostPreviewResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getTitle(),
                post.getSummary(),
                post.getGenre(),
                post.getPrice(),
                post.getCreatedAt()
        );
    }

    private DataPostResponse toDataPostResponse(DataPost post) {
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
                post.getVersion()
        );
    }


}
