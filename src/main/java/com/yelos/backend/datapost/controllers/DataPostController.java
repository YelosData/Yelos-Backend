package com.yelos.backend.datapost.controllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.yelos.backend.datapost.dto.DataPostPreviewResponse;
import com.yelos.backend.datapost.dto.DataPostResponse;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.services.DataPostService;
import com.yelos.backend.globaldto.PageResponse;

@RestController
@RequestMapping("/api/datapost")
public class DataPostController {

    private final DataPostService dataPostService;
    public DataPostController(DataPostService dataPostService) {
        this.dataPostService = dataPostService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DataPostResponse createPost(
            @RequestParam Long userId,
            @RequestParam String title,
            @RequestParam BigDecimal price,
            @RequestParam Genre genre,
            @RequestParam MultipartFile file
    ) {
        return dataPostService.postDataPosts(
                userId,
                title,
                price,
                genre,
                file
        );
    }

    @PutMapping(
            value = "/posts/{postId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public DataPostResponse updatePost(
            @PathVariable Long postId,
            @RequestParam Long userId,
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam BigDecimal price,
            @RequestParam Genre genre,
            @RequestParam(required = false) MultipartFile file
    ) {
        return dataPostService.updatePost(
                postId,
                userId,
                title,
                summary,
                genre,
                price,
                file
        );
    }

    @DeleteMapping("/posts/{postId}")
    public void deletePost(
            @PathVariable Long postId,
            @RequestParam Long userId
    ) {
        dataPostService.deletePost(postId, userId);
    }


    @GetMapping("/marketplace")
    public PageResponse<DataPostPreviewResponse> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Genre genre,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        return dataPostService.getDataPost(page, size, genre, sort);
    }

    @GetMapping("/user/{userId}")
    public PageResponse<DataPostResponse> getAllUserPost(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        return dataPostService.getAllDataPostsByUid(userId, page, size, sort);
    }

    @GetMapping("/posts/{id}")
    public DataPostResponse getDataPostById(@PathVariable long id) {
        return dataPostService.getDataPostById(id);
    }

    @GetMapping("/admin/posts/unreviewed")
    public PageResponse<DataPostResponse> getUnreviewedActivePosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        return dataPostService.getUnreviewedActivePosts(page, size, sort);
    }

    @GetMapping("/search")
        public List<DataPostPreviewResponse> searchPosts(
                @RequestParam String q,
                @RequestParam(defaultValue = "10") int limit
        ) {
        return dataPostService.searchDataPosts(q, limit);
        }


    @PatchMapping("/admin/posts/{postId}/review")
    public DataPostResponse reviewPost(
            @PathVariable Long postId,
            @RequestParam Long adminId,
            @RequestParam DataPostStatus status,
            @RequestParam(required = false) String note
    ) {
        return dataPostService.reviewPost(
                postId,
                adminId,
                status,
                note
        );
    }

//    @GetMapping("/genres")
//    public List<GenreReponse> getAllPostGenres() {
//        return dataPostService.getAllPostGenres();
//    }
//
//    @GetMapping("/genres/{genre}")
//    public List<DataPostPreviewResponse> getAllDataPostByGenre(@PathVariable String genre) {
//        return dataPostService.getDataPostByGenre(genre);
//    }

}
