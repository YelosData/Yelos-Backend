package com.yelos.backend.datapost.services;

import com.yelos.backend.ai.services.AiService;
import com.yelos.backend.datapost.dto.DataPostPreviewResponse;
import com.yelos.backend.datapost.dto.DataPostResponse;
import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.repo.DataPostRepo;
import com.yelos.backend.globaldto.PageResponse;
import com.yelos.backend.jobs.AiProcessingProducer;
import com.yelos.backend.storage.dto.StoredFile;
import com.yelos.backend.storage.services.StorageService;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import com.yelos.backend.user.repo.UserRepo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataPostServiceTest {

    private static final String PENDING_SUMMARY =
            "AI processing is pending. Please check back shortly.";

    @Mock
    private DataPostRepo dataPostRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private StorageService storageService;

    @Mock
    private AiService aiService;

    @Mock
    private AiProcessingProducer aiProcessingProducer;

    @InjectMocks
    private DataPostService dataPostService;

    @Nested
    class PostDataPostsTests {

        @Test
        void postDataPosts_validRequest_uploadsFileSavesPendingPostSendsAiJobAndReturnsResponse() {
            Long userId = 1L;

            AppUser seller = user(
                    userId,
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            MockMultipartFile file = datasetFile();

            StoredFile storedFile = new StoredFile(
                    "yelos-datasets",
                    "sellers/1/posts/test-file.csv",
                    "test.csv",
                    "text/csv",
                    36L,
                    Instant.now()
            );

            when(userRepo.findById(userId))
                    .thenReturn(Optional.of(seller));

            when(storageService.uploadDatasetFile(
                    eq(userId),
                    any(MultipartFile.class)
            )).thenReturn(storedFile);

            when(dataPostRepo.save(any(DataPost.class)))
                    .thenAnswer(invocation -> {
                        DataPost savedPost = invocation.getArgument(0);
                        ReflectionTestUtils.setField(savedPost, "id", 10L);
                        return savedPost;
                    });

            DataPostResponse response = dataPostService.postDataPosts(
                    userId,
                    "Coding Dataset",
                    BigDecimal.valueOf(30),
                    Genre.CODING,
                    file
            );

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.username()).isEqualTo("seller1");
            assertThat(response.title()).isEqualTo("Coding Dataset");
            assertThat(response.summary()).isEqualTo(PENDING_SUMMARY);
            assertThat(response.genre()).isEqualTo(Genre.CODING);
            assertThat(response.price())
                    .isEqualByComparingTo(BigDecimal.valueOf(30));
            assertThat(response.createdAt()).isNotNull();
            assertThat(response.fileName()).isEqualTo("test.csv");
            assertThat(response.fileType()).isEqualTo("text/csv");
            assertThat(response.fileSize()).isEqualTo(36L);
            assertThat(response.status()).isEqualTo(DataPostStatus.PENDING);

            // AI processing has not run yet.
            assertThat(response.previewCsv()).isNull();
            assertThat(response.aiFlagged()).isFalse();
            assertThat(response.aiFlagDetails()).isNull();
            assertThat(response.qualityRating()).isNull();

            verify(userRepo).findById(userId);

            verify(storageService).uploadDatasetFile(
                    eq(userId),
                    any(MultipartFile.class)
            );

            verify(dataPostRepo).save(argThat(post ->
                    post.getUser().equals(seller)
                            && post.getGenre() == Genre.CODING
                            && post.getTitle().equals("Coding Dataset")
                            && post.getSummary().equals(PENDING_SUMMARY)
                            && post.getPrice().compareTo(BigDecimal.valueOf(30)) == 0
                            && post.getStatus() == DataPostStatus.PENDING
                            && post.getFileName().equals("test.csv")
                            && post.getFileType().equals("text/csv")
                            && post.getFileSize().equals(36L)
                            && post.getBucketName().equals("yelos-datasets")
                            && post.getObjectKey().equals("sellers/1/posts/test-file.csv")
                            && post.getPreviewCsv() == null
                            && Boolean.FALSE.equals(post.getAiFlagged())
                            && post.getAiFlagDetails() == null
                            && post.getQualityCompleteness() == null
                            && post.getQualityConsistency() == null
                            && post.getQualityUniqueness() == null
                            && post.getQualityValidity() == null
                            && post.getQualityRichness() == null
                            && post.getQualityTotal() == null
            ));

            verify(aiProcessingProducer).send(argThat(message ->
                    message.postId().equals(10L)
                            && message.userId().equals(userId)
                            && message.bucketName().equals("yelos-datasets")
                            && message.objectKey().equals("sellers/1/posts/test-file.csv")
            ));

            verifyNoInteractions(aiService);
        }

        @Test
        void postDataPosts_userNotFound_throwsExceptionAndDoesNotUploadFile() {
            Long userId = 1L;
            MockMultipartFile file = datasetFile();

            when(userRepo.findById(userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> dataPostService.postDataPosts(
                    userId,
                    "Coding Dataset",
                    BigDecimal.valueOf(30),
                    Genre.CODING,
                    file
            ))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User does not exist");

            verify(userRepo).findById(userId);
            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
            verifyNoInteractions(aiProcessingProducer);
        }
    }

    @Nested
    class GetDataPostByIdTests {

        @Test
        void getDataPostById_existingPost_returnsResponseWithQualityRating() {
            AppUser seller = user(
                    1L,
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            DataPost post = post(
                    10L,
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            when(dataPostRepo.findById(10L))
                    .thenReturn(Optional.of(post));

            DataPostResponse response =
                    dataPostService.getDataPostById(10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.username()).isEqualTo("seller1");
            assertThat(response.title()).isEqualTo("Coding Dataset");
            assertThat(response.summary()).isEqualTo("Coding summary");
            assertThat(response.genre()).isEqualTo(Genre.CODING);
            assertThat(response.price())
                    .isEqualByComparingTo(BigDecimal.valueOf(30));
            assertThat(response.previewCsv())
                    .isEqualTo("id,name,value\n1,Alice,100");
            assertThat(response.aiFlagged()).isFalse();

            assertQualityRating(response);

            verify(dataPostRepo).findById(10L);
            verifyNoInteractions(userRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
            verifyNoInteractions(aiProcessingProducer);
        }

        @Test
        void getDataPostById_missingPost_throwsException() {
            when(dataPostRepo.findById(10L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    dataPostService.getDataPostById(10L)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Data post not found");

            verify(dataPostRepo).findById(10L);
            verifyNoInteractions(userRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
            verifyNoInteractions(aiProcessingProducer);
        }
    }

    @Nested
    class GetDataPostTests {

        @Test
        void getDataPost_nullGenre_usesFindByStatusAndReturnsPreviewPageWithRating() {
            AppUser seller = user(
                    1L,
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            DataPost post = post(
                    10L,
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            Page<DataPost> pageResult =
                    new PageImpl<>(List.of(post));

            when(dataPostRepo.findByStatus(
                    eq(DataPostStatus.ACTIVE),
                    any(Pageable.class)
            )).thenReturn(pageResult);

            PageResponse<DataPostPreviewResponse> response =
                    dataPostService.getDataPost(
                            0,
                            12,
                            null,
                            "newest"
                    );

            assertThat(response.content()).hasSize(1);

            DataPostPreviewResponse preview =
                    response.content().get(0);

            assertThat(preview.id()).isEqualTo(10L);
            assertThat(preview.title()).isEqualTo("Coding Dataset");
            assertThat(preview.summary()).isEqualTo("Coding summary");
            assertThat(preview.genre()).isEqualTo(Genre.CODING);
            assertThat(preview.qualityRating()).isNotNull();
            assertThat(preview.qualityRating().total())
                    .isEqualByComparingTo("90.60");

            verify(dataPostRepo).findByStatus(
                    eq(DataPostStatus.ACTIVE),
                    argThat((Pageable pageable) ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 12
                                    && pageable.getSort().getOrderFor("createdAt") != null
                                    && pageable.getSort()
                                    .getOrderFor("createdAt")
                                    .isDescending()
                    )
            );

            verify(dataPostRepo, never())
                    .findAll(any(Pageable.class));

            verify(dataPostRepo, never())
                    .findByGenre(any(), any(Pageable.class));

            verifyNoInteractions(userRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
            verifyNoInteractions(aiProcessingProducer);
        }

        @Test
        void getDataPost_specificGenre_usesFindByGenreAndStatus() {
            AppUser seller = user(
                    1L,
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            DataPost post = post(
                    10L,
                    seller,
                    Genre.FINANCE,
                    "Finance Dataset",
                    "Finance summary",
                    BigDecimal.valueOf(40)
            );

            Page<DataPost> pageResult =
                    new PageImpl<>(List.of(post));

            when(dataPostRepo.findByGenreAndStatus(
                    eq(Genre.FINANCE),
                    eq(DataPostStatus.ACTIVE),
                    any(Pageable.class)
            )).thenReturn(pageResult);

            PageResponse<DataPostPreviewResponse> response =
                    dataPostService.getDataPost(
                            0,
                            12,
                            Genre.FINANCE,
                            "priceAsc"
                    );

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).genre())
                    .isEqualTo(Genre.FINANCE);
            assertThat(response.content().get(0).title())
                    .isEqualTo("Finance Dataset");
            assertThat(response.content().get(0).qualityRating())
                    .isNotNull();

            verify(dataPostRepo).findByGenreAndStatus(
                    eq(Genre.FINANCE),
                    eq(DataPostStatus.ACTIVE),
                    argThat((Pageable pageable) ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 12
                                    && pageable.getSort().getOrderFor("price") != null
                                    && pageable.getSort()
                                    .getOrderFor("price")
                                    .isAscending()
                    )
            );

            verify(dataPostRepo, never())
                    .findAll(any(Pageable.class));

            verifyNoInteractions(userRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
            verifyNoInteractions(aiProcessingProducer);
        }

        @Test
        void getDataPost_invalidPageAndSize_normalizesValues() {
            Page<DataPost> emptyPage =
                    new PageImpl<>(List.of());

            when(dataPostRepo.findByStatus(
                    eq(DataPostStatus.ACTIVE),
                    any(Pageable.class)
            )).thenReturn(emptyPage);

            PageResponse<DataPostPreviewResponse> response =
                    dataPostService.getDataPost(
                            -5,
                            999,
                            null,
                            "newest"
                    );

            assertThat(response.content()).isEmpty();

            verify(dataPostRepo).findByStatus(
                    eq(DataPostStatus.ACTIVE),
                    argThat((Pageable pageable) ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 12
                    )
            );

            verifyNoInteractions(userRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
            verifyNoInteractions(aiProcessingProducer);
        }
    }

    @Nested
    class GetAllDataPostsByUidTests {

        @Test
        void getAllDataPostsByUid_validUser_returnsPageResponseWithRating() {
            Long userId = 1L;

            AppUser seller = user(
                    userId,
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            DataPost post = post(
                    10L,
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            Page<DataPost> pageResult =
                    new PageImpl<>(List.of(post));

            when(userRepo.findById(userId))
                    .thenReturn(Optional.of(seller));

            when(dataPostRepo.findByUserIdAndStatusNot(
                    eq(userId),
                    eq(DataPostStatus.DELETED),
                    any(Pageable.class)
            )).thenReturn(pageResult);

            PageResponse<DataPostResponse> response =
                    dataPostService.getAllDataPostsByUid(
                            userId,
                            0,
                            12,
                            "oldest"
                    );

            assertThat(response.content()).hasSize(1);

            DataPostResponse postResponse =
                    response.content().get(0);

            assertThat(postResponse.id()).isEqualTo(10L);
            assertThat(postResponse.userId()).isEqualTo(userId);
            assertThat(postResponse.title()).isEqualTo("Coding Dataset");
            assertThat(postResponse.fileName()).isEqualTo("test.csv");
            assertThat(postResponse.fileType()).isEqualTo("text/csv");
            assertThat(postResponse.fileSize()).isEqualTo(36L);
            assertThat(postResponse.previewCsv())
                    .isEqualTo("id,name,value\n1,Alice,100");
            assertThat(postResponse.aiFlagged()).isFalse();

            assertQualityRating(postResponse);

            verify(userRepo).findById(userId);

            verify(dataPostRepo).findByUserIdAndStatusNot(
                    eq(userId),
                    eq(DataPostStatus.DELETED),
                    argThat((Pageable pageable) ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 12
                                    && pageable.getSort().getOrderFor("createdAt") != null
                                    && pageable.getSort()
                                    .getOrderFor("createdAt")
                                    .isAscending()
                    )
            );

            verify(dataPostRepo, never())
                    .findByUserId(any(), any(Pageable.class));

            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
            verifyNoInteractions(aiProcessingProducer);
        }

        @Test
        void getAllDataPostsByUid_userNotFound_throwsException() {
            Long userId = 1L;

            when(userRepo.findById(userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    dataPostService.getAllDataPostsByUid(
                            userId,
                            0,
                            12,
                            "newest"
                    )
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User does not exist");

            verify(userRepo).findById(userId);
            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
            verifyNoInteractions(aiProcessingProducer);
        }
    }


    private void assertQualityRating(
            DataPostResponse response
    ) {
        assertThat(response.qualityRating()).isNotNull();
        assertThat(response.qualityRating().completeness())
                .isEqualByComparingTo("98.50");
        assertThat(response.qualityRating().consistency())
                .isEqualByComparingTo("92.00");
        assertThat(response.qualityRating().uniqueness())
                .isEqualByComparingTo("87.50");
        assertThat(response.qualityRating().validity())
                .isEqualByComparingTo("95.00");
        assertThat(response.qualityRating().richness())
                .isEqualByComparingTo("80.00");
        assertThat(response.qualityRating().total())
                .isEqualByComparingTo("90.60");
    }


    private MockMultipartFile datasetFile() {
        return new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                "id,name,value\n1,Alice,100\n2,Bob,200".getBytes()
        );
    }


    private AppUser user(
            Long id,
            String username,
            Role role,
            BigDecimal credits
    ) {
        AppUser user = new AppUser(
                username,
                username + "@example.com",
                "password123",
                credits,
                role,
                Instant.now(),
                Instant.now()
        );

        ReflectionTestUtils.setField(
                user,
                "id",
                id
        );

        return user;
    }


    private DataPost post(
            Long id,
            AppUser user,
            Genre genre,
            String title,
            String summary,
            BigDecimal price
    ) {
        DataPost post = new DataPost(
                user,
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
                "sellers/1/posts/test-file.csv"
        );

        ReflectionTestUtils.setField(
                post,
                "id",
                id
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

        return post;
    }
}
