package com.yelos.backend.datapost.controllers;

import com.yelos.backend.datapost.dto.DataPostPreviewResponse;
import com.yelos.backend.datapost.dto.DataPostResponse;
import com.yelos.backend.datapost.dto.DataQualityRatingResponse;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.services.DataPostService;
import com.yelos.backend.globaldto.PageResponse;
import com.yelos.backend.security.JwtService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        void createPost_validMultipartRequest_returnsDataPostResponse()
                throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.csv",
                    "text/csv",
                    "id,name,value\n1,Alice,100\n2,Bob,200".getBytes()
            );

            DataPostResponse response = dataPostResponse(
                    10L,
                    1L,
                    "seller1",
                    "Coding Dataset",
                    "Coding summary",
                    Genre.CODING,
                    BigDecimal.valueOf(30)
            );

            when(dataPostService.postDataPosts(
                    eq(1L),
                    eq("Coding Dataset"),
                    eq(BigDecimal.valueOf(30)),
                    eq(Genre.CODING),
                    any(MultipartFile.class)
            )).thenReturn(response);

            mockMvc.perform(multipart("/api/datapost")
                            .file(file)
                            .param("userId", "1")
                            .param("title", "Coding Dataset")
                            .param("price", "30")
                            .param("genre", "CODING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.userId").value(1L))
                    .andExpect(jsonPath("$.username").value("seller1"))
                    .andExpect(jsonPath("$.title").value("Coding Dataset"))
                    .andExpect(jsonPath("$.summary").value("Coding summary"))
                    .andExpect(jsonPath("$.genre").value("CODING"))
                    .andExpect(jsonPath("$.price").value(30))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.fileName").value("test.csv"))
                    .andExpect(jsonPath("$.fileType").value("text/csv"))
                    .andExpect(jsonPath("$.fileSize").value(36))
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.version").value(1))
                    .andExpect(jsonPath("$.adminReviewed").value(false))
                    .andExpect(jsonPath("$.previewCsv")
                            .value("id,name,value\n1,Alice,100"))
                    .andExpect(jsonPath("$.aiFlagged").value(false))
                    .andExpect(jsonPath("$.qualityRating.completeness")
                            .value(98.5))
                    .andExpect(jsonPath("$.qualityRating.consistency")
                            .value(92.0))
                    .andExpect(jsonPath("$.qualityRating.uniqueness")
                            .value(87.5))
                    .andExpect(jsonPath("$.qualityRating.validity")
                            .value(95.0))
                    .andExpect(jsonPath("$.qualityRating.richness")
                            .value(80.0))
                    .andExpect(jsonPath("$.qualityRating.total")
                            .value(90.6));

            ArgumentCaptor<MultipartFile> fileCaptor =
                    ArgumentCaptor.forClass(MultipartFile.class);

            verify(dataPostService).postDataPosts(
                    eq(1L),
                    eq("Coding Dataset"),
                    eq(BigDecimal.valueOf(30)),
                    eq(Genre.CODING),
                    fileCaptor.capture()
            );

            MultipartFile uploadedFile =
                    fileCaptor.getValue();

            assertThat(uploadedFile.getName())
                    .isEqualTo("file");
            assertThat(uploadedFile.getOriginalFilename())
                    .isEqualTo("test.csv");
            assertThat(uploadedFile.getContentType())
                    .isEqualTo("text/csv");
        }
    }

    @Nested
    class GetMarketplaceTests {

        @Test
        void getPosts_withParams_returnsPreviewPage()
                throws Exception {
            DataPostPreviewResponse preview =
                    dataPostPreviewResponse(
                            10L,
                            1L,
                            "seller1",
                            "Coding Dataset",
                            "Coding summary",
                            Genre.CODING,
                            BigDecimal.valueOf(30)
                    );

            PageResponse<DataPostPreviewResponse> pageResponse =
                    new PageResponse<>(
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
                    .andExpect(jsonPath("$.content[0].username")
                            .value("seller1"))
                    .andExpect(jsonPath("$.content[0].title")
                            .value("Coding Dataset"))
                    .andExpect(jsonPath("$.content[0].summary")
                            .value("Coding summary"))
                    .andExpect(jsonPath("$.content[0].genre")
                            .value("CODING"))
                    .andExpect(jsonPath("$.content[0].price")
                            .value(30))
                    .andExpect(jsonPath(
                            "$.content[0].qualityRating.completeness"
                    ).value(98.5))
                    .andExpect(jsonPath(
                            "$.content[0].qualityRating.consistency"
                    ).value(92.0))
                    .andExpect(jsonPath(
                            "$.content[0].qualityRating.uniqueness"
                    ).value(87.5))
                    .andExpect(jsonPath(
                            "$.content[0].qualityRating.validity"
                    ).value(95.0))
                    .andExpect(jsonPath(
                            "$.content[0].qualityRating.richness"
                    ).value(80.0))
                    .andExpect(jsonPath(
                            "$.content[0].qualityRating.total"
                    ).value(90.6))
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
        void getPosts_withoutParams_usesDefaults()
                throws Exception {
            PageResponse<DataPostPreviewResponse> pageResponse =
                    new PageResponse<>(
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
        void getAllUserPost_validUserId_returnsPageResponse()
                throws Exception {
            Long userId = 1L;

            DataPostResponse post = dataPostResponse(
                    10L,
                    userId,
                    "seller1",
                    "Coding Dataset",
                    "Coding summary",
                    Genre.CODING,
                    BigDecimal.valueOf(30)
            );

            PageResponse<DataPostResponse> pageResponse =
                    new PageResponse<>(
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

            mockMvc.perform(get(
                            "/api/datapost/user/{userId}",
                            userId
                    )
                            .param("page", "0")
                            .param("size", "12")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(10L))
                    .andExpect(jsonPath("$.content[0].userId").value(1L))
                    .andExpect(jsonPath("$.content[0].title")
                            .value("Coding Dataset"))
                    .andExpect(jsonPath("$.content[0].summary")
                            .value("Coding summary"))
                    .andExpect(jsonPath("$.content[0].fileName")
                            .value("test.csv"))
                    .andExpect(jsonPath("$.content[0].fileType")
                            .value("text/csv"))
                    .andExpect(jsonPath("$.content[0].fileSize")
                            .value(36))
                    .andExpect(jsonPath("$.content[0].status")
                            .value("ACTIVE"))
                    .andExpect(jsonPath("$.content[0].version")
                            .value(1))
                    .andExpect(jsonPath("$.content[0].adminReviewed")
                            .value(false))
                    .andExpect(jsonPath("$.content[0].previewCsv")
                            .value("id,name,value\n1,Alice,100"))
                    .andExpect(jsonPath("$.content[0].aiFlagged")
                            .value(false))
                    .andExpect(jsonPath(
                            "$.content[0].qualityRating.total"
                    ).value(90.6))
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
        void getDataPostById_validId_returnsDataPostResponse()
                throws Exception {
            long postId = 10L;

            DataPostResponse response = dataPostResponse(
                    postId,
                    1L,
                    "seller1",
                    "Coding Dataset",
                    "Coding summary",
                    Genre.CODING,
                    BigDecimal.valueOf(30)
            );

            when(dataPostService.getDataPostById(postId))
                    .thenReturn(response);

            mockMvc.perform(get(
                            "/api/datapost/posts/{id}",
                            postId
                    ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.userId").value(1L))
                    .andExpect(jsonPath("$.username").value("seller1"))
                    .andExpect(jsonPath("$.title")
                            .value("Coding Dataset"))
                    .andExpect(jsonPath("$.summary")
                            .value("Coding summary"))
                    .andExpect(jsonPath("$.genre")
                            .value("CODING"))
                    .andExpect(jsonPath("$.price")
                            .value(30))
                    .andExpect(jsonPath("$.createdAt")
                            .exists())
                    .andExpect(jsonPath("$.fileName")
                            .value("test.csv"))
                    .andExpect(jsonPath("$.fileType")
                            .value("text/csv"))
                    .andExpect(jsonPath("$.fileSize")
                            .value(36))
                    .andExpect(jsonPath("$.updatedAt")
                            .exists())
                    .andExpect(jsonPath("$.status")
                            .value("ACTIVE"))
                    .andExpect(jsonPath("$.version")
                            .value(1))
                    .andExpect(jsonPath("$.adminReviewed")
                            .value(false))
                    .andExpect(jsonPath("$.previewCsv")
                            .value("id,name,value\n1,Alice,100"))
                    .andExpect(jsonPath("$.aiFlagged")
                            .value(false))
                    .andExpect(jsonPath("$.qualityRating.completeness")
                            .value(98.5))
                    .andExpect(jsonPath("$.qualityRating.consistency")
                            .value(92.0))
                    .andExpect(jsonPath("$.qualityRating.uniqueness")
                            .value(87.5))
                    .andExpect(jsonPath("$.qualityRating.validity")
                            .value(95.0))
                    .andExpect(jsonPath("$.qualityRating.richness")
                            .value(80.0))
                    .andExpect(jsonPath("$.qualityRating.total")
                            .value(90.6));

            verify(dataPostService)
                    .getDataPostById(postId);
        }
    }


    private DataPostResponse dataPostResponse(
            Long id,
            Long userId,
            String username,
            String title,
            String summary,
            Genre genre,
            BigDecimal price
    ) {
        Instant now = Instant.now();

        return new DataPostResponse(
                id,
                userId,
                username,
                title,
                summary,
                genre,
                price,
                now,
                "test.csv",
                "text/csv",
                36L,
                now,
                DataPostStatus.ACTIVE,
                1,
                false,
                null,
                null,
                null,
                "id,name,value\n1,Alice,100",
                false,
                null,
                qualityRatingResponse()
        );
    }


    private DataPostPreviewResponse dataPostPreviewResponse(
            long id,
            long userId,
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
                Instant.now(),
                false,
                qualityRatingResponse()
        );
    }


    private DataQualityRatingResponse qualityRatingResponse() {
        return new DataQualityRatingResponse(
                new BigDecimal("98.50"),
                new BigDecimal("92.00"),
                new BigDecimal("87.50"),
                new BigDecimal("95.00"),
                new BigDecimal("80.00"),
                new BigDecimal("90.60")
        );
    }
}
