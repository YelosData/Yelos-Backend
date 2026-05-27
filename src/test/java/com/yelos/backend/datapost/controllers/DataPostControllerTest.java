package com.yelos.backend.datapost.controllers;

import com.yelos.backend.datapost.dto.DataPostPreviewResponse;
import com.yelos.backend.datapost.dto.DataPostResponse;
import com.yelos.backend.datapost.dto.PostRequest;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.services.DataPostService;
import com.yelos.backend.globaldto.PageResponse;
import com.yelos.backend.security.JwtService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataPostController.class)
@AutoConfigureMockMvc(addFilters = false)
class DataPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataPostService dataPostService;

    @MockitoBean
    private JwtService jwtService;

    @Nested
    class CreatePostTests {

        @Test
        void createPost_validRequest_returnsDataPostResponse() throws Exception {
            DataPostResponse response = dataPostResponse(
                    10L,
                    1L,
                    "seller1",
                    "Coding Dataset",
                    "Coding summary",
                    "Full coding content",
                    Genre.CODING,
                    BigDecimal.valueOf(30)
            );

            when(dataPostService.postDataPosts(any(PostRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/datapost")
                            .contentType(APPLICATION_JSON)
                            .content("""
                            {
                              "userId": 1,
                              "title": "Coding Dataset",
                              "content": "Full coding content",
                              "summary": "Coding summary",
                              "price": 30,
                              "genre": "CODING"
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.userId").value(1L))
                    .andExpect(jsonPath("$.username").value("seller1"))
                    .andExpect(jsonPath("$.title").value("Coding Dataset"))
                    .andExpect(jsonPath("$.summary").value("Coding summary"))
                    .andExpect(jsonPath("$.content").value("Full coding content"))
                    .andExpect(jsonPath("$.genre").value("CODING"))
                    .andExpect(jsonPath("$.price").value(30))
                    .andExpect(jsonPath("$.createdAt").exists());

            verify(dataPostService).postDataPosts(argThat(request ->
                    request.userId().equals(1L)
                            && request.title().equals("Coding Dataset")
                            && request.content().equals("Full coding content")
                            && request.summary().equals("Coding summary")
                            && request.price().compareTo(BigDecimal.valueOf(30)) == 0
                            && request.genre() == Genre.CODING
            ));
        }
    }

    @Nested
    class GetMarketplaceTests {

        @Test
        void getPosts_withParams_returnsPreviewPage() throws Exception {
            DataPostPreviewResponse preview = dataPostPreviewResponse(
                    10L,
                    1L,
                    "seller1",
                    "Coding Dataset",
                    "Coding summary",
                    Genre.CODING,
                    BigDecimal.valueOf(30)
            );

            PageResponse<DataPostPreviewResponse> pageResponse = new PageResponse<>(
                    List.of(preview),
                    0,
                    6,
                    1,
                    1,
                    true
            );

            when(dataPostService.getDataPost(
                    eq(0),
                    eq(6),
                    eq(Genre.CODING),
                    eq("priceAsc")
            )).thenReturn(pageResponse);

            mockMvc.perform(get("/api/datapost/marketplace")
                            .param("page", "0")
                            .param("size", "6")
                            .param("genre", "CODING")
                            .param("sort", "priceAsc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(10L))
                    .andExpect(jsonPath("$.content[0].userId").value(1L))
                    .andExpect(jsonPath("$.content[0].username").value("seller1"))
                    .andExpect(jsonPath("$.content[0].title").value("Coding Dataset"))
                    .andExpect(jsonPath("$.content[0].summary").value("Coding summary"))
                    .andExpect(jsonPath("$.content[0].genre").value("CODING"))
                    .andExpect(jsonPath("$.content[0].price").value(30))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(6))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.last").value(true));

            verify(dataPostService).getDataPost(
                    eq(0),
                    eq(6),
                    eq(Genre.CODING),
                    eq("priceAsc")
            );
        }

        @Test
        void getPosts_withoutParams_usesDefaults() throws Exception {
            PageResponse<DataPostPreviewResponse> pageResponse = new PageResponse<>(
                    List.of(),
                    0,
                    12,
                    0,
                    0,
                    true
            );

            when(dataPostService.getDataPost(
                    eq(0),
                    eq(12),
                    isNull(),
                    eq("newest")
            )).thenReturn(pageResponse);

            mockMvc.perform(get("/api/datapost/marketplace"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(12))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.last").value(true));

            verify(dataPostService).getDataPost(
                    eq(0),
                    eq(12),
                    isNull(),
                    eq("newest")
            );
        }
    }

    @Nested
    class GetAllUserPostTests {

        @Test
        void getAllUserPost_validUserId_returnsPageResponse() throws Exception {
            Long userId = 1L;

            DataPostResponse post = dataPostResponse(
                    10L,
                    userId,
                    "seller1",
                    "Coding Dataset",
                    "Coding summary",
                    "Full coding content",
                    Genre.CODING,
                    BigDecimal.valueOf(30)
            );

            PageResponse<DataPostResponse> pageResponse = new PageResponse<>(
                    List.of(post),
                    0,
                    12,
                    1,
                    1,
                    true
            );

            when(dataPostService.getAllDataPostsByUid(
                    eq(userId),
                    eq(0),
                    eq(12),
                    eq("newest")
            )).thenReturn(pageResponse);

            mockMvc.perform(get("/api/datapost/user/{userId}", userId)
                            .param("page", "0")
                            .param("size", "12")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(10L))
                    .andExpect(jsonPath("$.content[0].userId").value(1L))
                    .andExpect(jsonPath("$.content[0].title").value("Coding Dataset"))
                    .andExpect(jsonPath("$.content[0].content").value("Full coding content"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(12));

            verify(dataPostService).getAllDataPostsByUid(
                    eq(userId),
                    eq(0),
                    eq(12),
                    eq("newest")
            );
        }
    }

    @Nested
    class GetDataPostByIdTests {

        @Test
        void getDataPostById_validId_returnsDataPostResponse() throws Exception {
            long postId = 10L;

            DataPostResponse response = dataPostResponse(
                    postId,
                    1L,
                    "seller1",
                    "Coding Dataset",
                    "Coding summary",
                    "Full coding content",
                    Genre.CODING,
                    BigDecimal.valueOf(30)
            );

            when(dataPostService.getDataPostById(postId)).thenReturn(response);

            mockMvc.perform(get("/api/datapost/posts/{id}", postId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.userId").value(1L))
                    .andExpect(jsonPath("$.username").value("seller1"))
                    .andExpect(jsonPath("$.title").value("Coding Dataset"))
                    .andExpect(jsonPath("$.summary").value("Coding summary"))
                    .andExpect(jsonPath("$.content").value("Full coding content"))
                    .andExpect(jsonPath("$.genre").value("CODING"))
                    .andExpect(jsonPath("$.price").value(30))
                    .andExpect(jsonPath("$.createdAt").exists());

            verify(dataPostService).getDataPostById(postId);
        }
    }

    private DataPostResponse dataPostResponse(
            Long id,
            Long userId,
            String username,
            String title,
            String summary,
            String content,
            Genre genre,
            BigDecimal price
    ) {
        return new DataPostResponse(
                id,
                userId,
                username,
                title,
                summary,
                content,
                genre,
                price,
                Instant.now()
        );
    }

    private DataPostPreviewResponse dataPostPreviewResponse(
            Long id,
            Long userId,
            String username,
            String title,
            String summary,
            Genre genre,
            BigDecimal price
    ) {
        return new DataPostPreviewResponse(
                id,
                userId,
                username,
                title,
                summary,
                genre,
                price,
                Instant.now()
        );
    }
}