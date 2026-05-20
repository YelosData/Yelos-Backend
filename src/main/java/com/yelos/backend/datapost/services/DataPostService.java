package com.yelos.backend.datapost.services;


import com.yelos.backend.datapost.dto.DataPostPreviewResponse;
import com.yelos.backend.datapost.dto.PostRequest;
import com.yelos.backend.datapost.dto.DataPostResponse;
import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.repo.DataPostRepo;
import com.yelos.backend.globaldto.PageResponse;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.repo.UserRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;


@Service
public class DataPostService {
    private final DataPostRepo dataPostRepo;
    private final UserRepo userRepo;

    public DataPostService(UserRepo userRepo, DataPostRepo dataPostRepo){
        this.userRepo = userRepo;
        this.dataPostRepo = dataPostRepo;
    }


    public DataPostResponse postDataPosts(PostRequest request){
        AppUser user = userRepo.findById(request.userId()).orElseThrow(() -> new RuntimeException("User does not exists"));
        Genre genre = request.genre();
        DataPost post = new DataPost(
                user,
                genre,
                request.title(),
                request.content(),
                request.summary(),
                request.price(),
                DataPostStatus.ACTIVE,
                Instant.now()
        );
        DataPost savedPost = dataPostRepo.save(post);
        return new DataPostResponse(
                post.getId(),
                user.getId(),
                user.getUsername(),
                post.getTitle(),
                post.getSummary(),
                post.getContent(),
                post.getGenre(),
                post.getPrice(),
                post.getCreatedAt()
        );
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
                ? dataPostRepo.findAll(pageable)
                : dataPostRepo.findByGenre(genre, pageable);

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

        Page<DataPost> result = dataPostRepo.findByUserId(user.getId(), pageable);

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
                post.getContent(),
                post.getGenre(),
                post.getPrice(),
                post.getCreatedAt()
        );
    }
}
