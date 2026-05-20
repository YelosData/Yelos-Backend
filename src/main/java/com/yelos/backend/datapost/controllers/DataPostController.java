package com.yelos.backend.datapost.controllers;

import com.yelos.backend.datapost.dto.DataPostPreviewResponse;
import com.yelos.backend.datapost.dto.GenreReponse;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.services.DataPostService;
import com.yelos.backend.datapost.dto.DataPostResponse;
import com.yelos.backend.datapost.dto.PostRequest;
import com.yelos.backend.globaldto.PageResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datapost")
public class DataPostController {

    private final DataPostService dataPostService;
    public DataPostController(DataPostService dataPostService) {
        this.dataPostService = dataPostService;
    }

    @PostMapping
    public DataPostResponse createPost(@RequestBody PostRequest request) {
        return dataPostService.postDataPosts(request);
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
