package com.yelos.backend.datapost;

import com.yelos.backend.IntegrationTestBase;
import com.yelos.backend.ai.services.AiService;
import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.jobs.AiProcessingProducer;
import com.yelos.backend.storage.dto.StoredFile;
import com.yelos.backend.storage.services.StorageService;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataPostIntegrationTest extends IntegrationTestBase {

    private static final String PENDING_SUMMARY =
            "AI processing is pending. Please check back shortly.";

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private AiService aiService;

    @MockitoBean
    private AiProcessingProducer aiProcessingProducer;

    @Nested
    class CreatePostTests {

        @Test
        void createPost_validRequest_savesPendingPostSendsAiJobAndReturnsResponse()
                throws Exception {
            AppUser seller = saveUser(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.csv",
                    "text/csv",
                    "id,name,value\n1,Alice,100\n2,Bob,200".getBytes()
            );

            when(storageService.uploadDatasetFile(
                    eq(seller.getId()),
                    any(MultipartFile.class)
            )).thenReturn(new StoredFile(
                    "yelos-datasets",
                    "sellers/" + seller.getId() + "/posts/test-file.csv",
                    "test.csv",
                    "text/csv",
                    36L,
                    Instant.now()
            ));

            mockMvc.perform(multipart("/api/datapost")
                            .file(file)
                            .param(
                                    "userId",
                                    String.valueOf(seller.getId())
                            )
                            .param("title", "Coding Dataset")
                            .param("price", "30")
                            .param("genre", "CODING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId")
                            .value(seller.getId()))
                    .andExpect(jsonPath("$.title")
                            .value("Coding Dataset"))
                    .andExpect(jsonPath("$.summary")
                            .value(PENDING_SUMMARY))
                    .andExpect(jsonPath("$.genre")
                            .value("CODING"))
                    .andExpect(jsonPath("$.price")
                            .value(30))
                    .andExpect(jsonPath("$.fileName")
                            .value("test.csv"))
                    .andExpect(jsonPath("$.fileType")
                            .value("text/csv"))
                    .andExpect(jsonPath("$.fileSize")
                            .value(36))
                    .andExpect(jsonPath("$.status")
                            .value("PENDING"))
                    .andExpect(jsonPath("$.previewCsv")
                            .doesNotExist())
                    .andExpect(jsonPath("$.aiFlagged")
                            .value(false))
                    .andExpect(jsonPath("$.aiFlagDetails")
                            .doesNotExist())
                    .andExpect(jsonPath("$.qualityRating")
                            .doesNotExist());

            assertThat(dataPostRepo.findAll()).hasSize(1);

            DataPost savedPost =
                    dataPostRepo.findAll().get(0);

            assertThat(savedPost.getUser().getId())
                    .isEqualTo(seller.getId());
            assertThat(savedPost.getTitle())
                    .isEqualTo("Coding Dataset");
            assertThat(savedPost.getSummary())
                    .isEqualTo(PENDING_SUMMARY);
            assertThat(savedPost.getGenre())
                    .isEqualTo(Genre.CODING);
            assertThat(savedPost.getPrice())
                    .isEqualByComparingTo(BigDecimal.valueOf(30));
            assertThat(savedPost.getStatus())
                    .isEqualTo(DataPostStatus.PENDING);
            assertThat(savedPost.getCreatedAt())
                    .isNotNull();

            assertThat(savedPost.getFileName())
                    .isEqualTo("test.csv");
            assertThat(savedPost.getFileType())
                    .isEqualTo("text/csv");
            assertThat(savedPost.getFileSize())
                    .isEqualTo(36L);
            assertThat(savedPost.getBucketName())
                    .isEqualTo("yelos-datasets");
            assertThat(savedPost.getObjectKey())
                    .isEqualTo(
                            "sellers/"
                                    + seller.getId()
                                    + "/posts/test-file.csv"
                    );

            // The queued worker has not run yet.
            assertThat(savedPost.getPreviewCsv()).isNull();
            assertThat(savedPost.getAiFlagged()).isFalse();
            assertThat(savedPost.getAiFlagDetails()).isNull();
            assertThat(savedPost.getQualityCompleteness()).isNull();
            assertThat(savedPost.getQualityConsistency()).isNull();
            assertThat(savedPost.getQualityUniqueness()).isNull();
            assertThat(savedPost.getQualityValidity()).isNull();
            assertThat(savedPost.getQualityRichness()).isNull();
            assertThat(savedPost.getQualityTotal()).isNull();

            verify(storageService).uploadDatasetFile(
                    eq(seller.getId()),
                    any(MultipartFile.class)
            );

            verify(aiProcessingProducer).send(argThat(message ->
                    message.postId().equals(savedPost.getId())
                            && message.userId().equals(seller.getId())
                            && message.bucketName().equals("yelos-datasets")
                            && message.objectKey().equals(
                            "sellers/"
                                    + seller.getId()
                                    + "/posts/test-file.csv"
                    )
            ));

            verifyNoInteractions(aiService);
        }

        @Test
        void createPost_missingUser_returnsNotFoundAndDoesNotSavePost()
                throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.csv",
                    "text/csv",
                    "id,name,value\n1,Alice,100\n2,Bob,200".getBytes()
            );

            mockMvc.perform(multipart("/api/datapost")
                            .file(file)
                            .param("userId", "999")
                            .param("title", "Coding Dataset")
                            .param("price", "30")
                            .param("genre", "CODING"))
                    .andExpect(status().isNotFound());

            assertThat(dataPostRepo.findAll()).isEmpty();

            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
            verifyNoInteractions(aiProcessingProducer);
        }
    }

    @Nested
    class MarketplaceTests {

        @Test
        void getMarketplace_withoutGenre_returnsAllPreviewPostsWithRatings()
                throws Exception {
            AppUser seller = saveUser(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            savePost(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            savePost(
                    seller,
                    Genre.FINANCE,
                    "Finance Dataset",
                    "Finance summary",
                    BigDecimal.valueOf(40)
            );

            mockMvc.perform(get("/api/datapost/marketplace")
                            .param("page", "0")
                            .param("size", "12")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()")
                            .value(2))
                    .andExpect(jsonPath("$.content[0].qualityRating")
                            .exists())
                    .andExpect(jsonPath("$.content[0].qualityRating.total")
                            .value(90.6))
                    .andExpect(jsonPath("$.content[1].qualityRating")
                            .exists())
                    .andExpect(jsonPath("$.page")
                            .value(0))
                    .andExpect(jsonPath("$.size")
                            .value(12))
                    .andExpect(jsonPath("$.totalElements")
                            .value(2));
        }

        @Test
        void getMarketplace_withGenre_returnsOnlyMatchingGenreAndRating()
                throws Exception {
            AppUser seller = saveUser(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            savePost(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            savePost(
                    seller,
                    Genre.FINANCE,
                    "Finance Dataset",
                    "Finance summary",
                    BigDecimal.valueOf(40)
            );

            mockMvc.perform(get("/api/datapost/marketplace")
                            .param("page", "0")
                            .param("size", "12")
                            .param("genre", "CODING")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()")
                            .value(1))
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
                    .andExpect(jsonPath("$.totalElements")
                            .value(1));
        }

        @Test
        void getMarketplace_invalidPageAndSize_areNormalisedByService()
                throws Exception {
            AppUser seller = saveUser(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            savePost(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            mockMvc.perform(get("/api/datapost/marketplace")
                            .param("page", "-5")
                            .param("size", "999")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page")
                            .value(0))
                    .andExpect(jsonPath("$.size")
                            .value(12))
                    .andExpect(jsonPath("$.totalElements")
                            .value(1));
        }
    }

    @Nested
    class GetUserPostsTests {

        @Test
        void getAllUserPost_returnsOnlyPostsByThatUserWithRating()
                throws Exception {
            AppUser seller1 = saveUser(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            AppUser seller2 = saveUser(
                    "seller2",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            savePost(
                    seller1,
                    Genre.CODING,
                    "Seller 1 Dataset",
                    "Summary 1",
                    BigDecimal.valueOf(30)
            );

            savePost(
                    seller2,
                    Genre.FINANCE,
                    "Seller 2 Dataset",
                    "Summary 2",
                    BigDecimal.valueOf(40)
            );

            mockMvc.perform(get(
                            "/api/datapost/user/{userId}",
                            seller1.getId()
                    )
                            .param("page", "0")
                            .param("size", "12")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()")
                            .value(1))
                    .andExpect(jsonPath("$.content[0].title")
                            .value("Seller 1 Dataset"))
                    .andExpect(jsonPath("$.content[0].userId")
                            .value(seller1.getId()))
                    .andExpect(jsonPath(
                            "$.content[0].qualityRating.total"
                    ).value(90.6))
                    .andExpect(jsonPath("$.totalElements")
                            .value(1));
        }

        @Test
        void getAllUserPost_missingUser_returnsNotFound()
                throws Exception {
            mockMvc.perform(get(
                            "/api/datapost/user/{userId}",
                            999L
                    )
                            .param("page", "0")
                            .param("size", "12")
                            .param("sort", "newest"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetDataPostByIdTests {

        @Test
        void getDataPostById_existingPost_returnsFullPostWithRating()
                throws Exception {
            AppUser seller = saveUser(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            DataPost post = savePost(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            mockMvc.perform(get(
                            "/api/datapost/posts/{id}",
                            post.getId()
                    ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id")
                            .value(post.getId()))
                    .andExpect(jsonPath("$.userId")
                            .value(seller.getId()))
                    .andExpect(jsonPath("$.username")
                            .value("seller1"))
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
                    .andExpect(jsonPath("$.previewCsv")
                            .value("id,name,value\n1,Alice,100"))
                    .andExpect(jsonPath("$.aiFlagged")
                            .value(false))
                    .andExpect(jsonPath(
                            "$.qualityRating.completeness"
                    ).value(98.5))
                    .andExpect(jsonPath(
                            "$.qualityRating.consistency"
                    ).value(92.0))
                    .andExpect(jsonPath(
                            "$.qualityRating.uniqueness"
                    ).value(87.5))
                    .andExpect(jsonPath(
                            "$.qualityRating.validity"
                    ).value(95.0))
                    .andExpect(jsonPath(
                            "$.qualityRating.richness"
                    ).value(80.0))
                    .andExpect(jsonPath(
                            "$.qualityRating.total"
                    ).value(90.6));
        }

        @Test
        void getDataPostById_missingPost_returnsNotFound()
                throws Exception {
            mockMvc.perform(get(
                            "/api/datapost/posts/{id}",
                            999L
                    ))
                    .andExpect(status().isNotFound());
        }
    }


    private AppUser saveUser(
            String username,
            Role role,
            BigDecimal credits
    ) {
        return userRepo.save(new AppUser(
                username,
                username + "@example.com",
                "password123",
                credits,
                role,
                Instant.now(),
                Instant.now()
        ));
    }


    private DataPost savePost(
            AppUser seller,
            Genre genre,
            String title,
            String summary,
            BigDecimal price
    ) {
        DataPost post = new DataPost(
                seller,
                genre,
                title,
                summary,
                price,
                DataPostStatus.ACTIVE,
                Instant.now(),
                "test.csv",
                "text/csv",
                36L,
                "yelos-datasets",
                "sellers/"
                        + seller.getId()
                        + "/posts/"
                        + UUID.randomUUID()
                        + "-test.csv"
        );

        post.attachAiMetadata(
                summary,
                "id,name,value\n1,Alice,100",
                false,
                null
        );

        post.attachQualityRating(
                new BigDecimal("98.50"),
                new BigDecimal("92.00"),
                new BigDecimal("87.50"),
                new BigDecimal("95.00"),
                new BigDecimal("80.00"),
                new BigDecimal("90.60")
        );

        return dataPostRepo.save(post);
    }
}
