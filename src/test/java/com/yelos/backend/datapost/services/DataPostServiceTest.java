package com.yelos.backend.datapost.services;

import com.yelos.backend.ai.dto.AiPipelineResponse;
import com.yelos.backend.ai.services.AiService;
import com.yelos.backend.datapost.dto.DataPostPreviewResponse;
import com.yelos.backend.datapost.dto.DataPostResponse;
import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.repo.DataPostRepo;
import com.yelos.backend.globaldto.PageResponse;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataPostServiceTest {

    @Mock
    private DataPostRepo dataPostRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private StorageService storageService;

    @Mock
    private AiService aiService;

    @InjectMocks
    private DataPostService dataPostService;

    @Nested
    class PostDataPostsTests {

        @Test
        void postDataPosts_validRequest_uploadsFileSavesPostAndReturnsResponse() {
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

            when(userRepo.findById(userId)).thenReturn(Optional.of(seller));
            mockAiPipeline();

            when(storageService.uploadDatasetBytes(
                    eq(userId),
                    eq("test.csv"),
                    eq("text/csv"),
                    any(byte[].class)
            )).thenReturn(storedFile);

            when(dataPostRepo.save(any(DataPost.class))).thenAnswer(invocation -> {
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
            assertThat(response.summary()).isEqualTo("Coding summary");
            assertThat(response.genre()).isEqualTo(Genre.CODING);
            assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(30));
            assertThat(response.createdAt()).isNotNull();
            assertThat(response.fileName()).isEqualTo("test.csv");
            assertThat(response.fileType()).isEqualTo("text/csv");
            assertThat(response.fileSize()).isEqualTo(36L);
            assertThat(response.status()).isEqualTo(DataPostStatus.ACTIVE);
            assertThat(response.previewCsv()).isEqualTo("id,name,value\n1,Alice,100");
            assertThat(response.aiFlagged()).isFalse();
            assertThat(response.aiFlagDetails()).isNull();

            verify(userRepo).findById(userId);
            verify(aiService).runPipeline(file);

            verify(storageService).uploadDatasetBytes(
                    eq(userId),
                    eq("test.csv"),
                    eq("text/csv"),
                    any(byte[].class)
            );

            verify(dataPostRepo).save(argThat(post ->
                    post.getUser().equals(seller)
                            && post.getGenre() == Genre.CODING
                            && post.getTitle().equals("Coding Dataset")
                            && post.getSummary().equals("Coding summary")
                            && post.getPrice().compareTo(BigDecimal.valueOf(30)) == 0
                            && post.getStatus() == DataPostStatus.ACTIVE
                            && post.getFileName().equals("test.csv")
                            && post.getFileType().equals("text/csv")
                            && post.getFileSize().equals(36L)
                            && post.getBucketName().equals("yelos-datasets")
                            && post.getObjectKey().equals("sellers/1/posts/test-file.csv")
                            && post.getPreviewCsv().equals("id,name,value\n1,Alice,100")
                            && Boolean.FALSE.equals(post.getAiFlagged())
                            && post.getAiFlagDetails() == null
            ));
        }

        @Test
        void postDataPosts_userNotFound_throwsExceptionAndDoesNotUploadFile() {
            Long userId = 1L;
            MockMultipartFile file = datasetFile();

            when(userRepo.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dataPostService.postDataPosts(
                    userId,
                    "Coding Dataset",
                    BigDecimal.valueOf(30),
                    Genre.CODING,
                    file
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User does not exist");

            verify(userRepo).findById(userId);
            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
        }
    }

    @Nested
    class GetDataPostByIdTests {

        @Test
        void getDataPostById_existingPost_returnsResponse() {
            AppUser seller = user(1L, "seller1", Role.SELLER, BigDecimal.ZERO);

            DataPost post = post(
                    10L,
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            when(dataPostRepo.findById(10L)).thenReturn(Optional.of(post));

            DataPostResponse response = dataPostService.getDataPostById(10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.username()).isEqualTo("seller1");
            assertThat(response.title()).isEqualTo("Coding Dataset");
            assertThat(response.summary()).isEqualTo("Coding summary");
            assertThat(response.genre()).isEqualTo(Genre.CODING);
            assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(30));
            assertThat(response.previewCsv()).isEqualTo("id,name,value\n1,Alice,100");
            assertThat(response.aiFlagged()).isFalse();

            verify(dataPostRepo).findById(10L);
            verifyNoInteractions(userRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
        }

        @Test
        void getDataPostById_missingPost_throwsException() {
            when(dataPostRepo.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dataPostService.getDataPostById(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Data post not found");

            verify(dataPostRepo).findById(10L);
            verifyNoInteractions(userRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
        }
    }

    @Nested
    class GetDataPostTests {

        @Test
        void getDataPost_nullGenre_usesFindAllAndReturnsPreviewPage() {
            AppUser seller = user(1L, "seller1", Role.SELLER, BigDecimal.ZERO);

            DataPost post = post(
                    10L,
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            Page<DataPost> pageResult = new PageImpl<>(List.of(post));

            when(dataPostRepo.findByStatus(eq(DataPostStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(pageResult);

            PageResponse<DataPostPreviewResponse> response = dataPostService.getDataPost(
                    0,
                    12,
                    null,
                    "newest"
            );

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).id()).isEqualTo(10L);
            assertThat(response.content().get(0).title()).isEqualTo("Coding Dataset");
            assertThat(response.content().get(0).summary()).isEqualTo("Coding summary");
            assertThat(response.content().get(0).genre()).isEqualTo(Genre.CODING);

            verify(dataPostRepo).findByStatus(
                    eq(DataPostStatus.ACTIVE),
                    argThat((Pageable pageable) ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 12
                                    && pageable.getSort().getOrderFor("createdAt") != null
                                    && pageable.getSort().getOrderFor("createdAt").isDescending()
                    )
            );

            verify(dataPostRepo, never()).findAll(any(Pageable.class));
            verify(dataPostRepo, never()).findByGenre(any(), any(Pageable.class));
            verifyNoInteractions(userRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
        }

        @Test
        void getDataPost_specificGenre_usesFindByGenre() {
            AppUser seller = user(1L, "seller1", Role.SELLER, BigDecimal.ZERO);

            DataPost post = post(
                    10L,
                    seller,
                    Genre.FINANCE,
                    "Finance Dataset",
                    "Finance summary",
                    BigDecimal.valueOf(40)
            );

            Page<DataPost> pageResult = new PageImpl<>(List.of(post));

            when(dataPostRepo.findByGenreAndStatus(
                    eq(Genre.FINANCE),
                    eq(DataPostStatus.ACTIVE),
                    any(Pageable.class)
            )).thenReturn(pageResult);

            PageResponse<DataPostPreviewResponse> response = dataPostService.getDataPost(
                    0,
                    12,
                    Genre.FINANCE,
                    "priceAsc"
            );

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).genre()).isEqualTo(Genre.FINANCE);
            assertThat(response.content().get(0).title()).isEqualTo("Finance Dataset");

            verify(dataPostRepo).findByGenreAndStatus(
                    eq(Genre.FINANCE),
                    eq(DataPostStatus.ACTIVE),
                    argThat((Pageable pageable) ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 12
                                    && pageable.getSort().getOrderFor("price") != null
                                    && pageable.getSort().getOrderFor("price").isAscending()
                    )
            );

            verify(dataPostRepo, never()).findAll(any(Pageable.class));
            verifyNoInteractions(userRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
        }

        @Test
        void getDataPost_invalidPageAndSize_normalizesValues() {
            Page<DataPost> emptyPage = new PageImpl<>(List.of());

            when(dataPostRepo.findByStatus(eq(DataPostStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(emptyPage);

            PageResponse<DataPostPreviewResponse> response = dataPostService.getDataPost(
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
        }
    }

    @Nested
    class GetAllDataPostsByUidTests {

        @Test
        void getAllDataPostsByUid_validUser_returnsPageResponse() {
            Long userId = 1L;

            AppUser seller = user(userId, "seller1", Role.SELLER, BigDecimal.ZERO);

            DataPost post = post(
                    10L,
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            Page<DataPost> pageResult = new PageImpl<>(List.of(post));

            when(userRepo.findById(userId)).thenReturn(Optional.of(seller));
            when(dataPostRepo.findByUserIdAndStatusNot(
                    eq(userId),
                    eq(DataPostStatus.DELETED),
                    any(Pageable.class)
            )).thenReturn(pageResult);

            PageResponse<DataPostResponse> response = dataPostService.getAllDataPostsByUid(
                    userId,
                    0,
                    12,
                    "oldest"
            );

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).id()).isEqualTo(10L);
            assertThat(response.content().get(0).userId()).isEqualTo(userId);
            assertThat(response.content().get(0).title()).isEqualTo("Coding Dataset");
            assertThat(response.content().get(0).fileName()).isEqualTo("test.csv");
            assertThat(response.content().get(0).fileType()).isEqualTo("text/csv");
            assertThat(response.content().get(0).fileSize()).isEqualTo(36L);
            assertThat(response.content().get(0).previewCsv()).isEqualTo("id,name,value\n1,Alice,100");
            assertThat(response.content().get(0).aiFlagged()).isFalse();

            verify(userRepo).findById(userId);

            verify(dataPostRepo).findByUserIdAndStatusNot(
                    eq(userId),
                    eq(DataPostStatus.DELETED),
                    argThat((Pageable pageable) ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 12
                                    && pageable.getSort().getOrderFor("createdAt") != null
                                    && pageable.getSort().getOrderFor("createdAt").isAscending()
                    )
            );

            verify(dataPostRepo, never()).findByUserId(any(), any(Pageable.class));

            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
        }

        @Test
        void getAllDataPostsByUid_userNotFound_throwsException() {
            Long userId = 1L;

            when(userRepo.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    dataPostService.getAllDataPostsByUid(userId, 0, 12, "newest")
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User does not exist");

            verify(userRepo).findById(userId);
            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(storageService);
            verifyNoInteractions(aiService);
        }
    }

    private void mockAiPipeline() {
        AiPipelineResponse aiResult = Mockito.mock(AiPipelineResponse.class, RETURNS_DEEP_STUBS);

        String csvContent = "id,name,value\n1,Alice,100\n2,Bob,200";
        String base64Content = Base64.getEncoder().encodeToString(csvContent.getBytes());

        lenient().when(aiService.runPipeline(any(MultipartFile.class))).thenReturn(aiResult);

        lenient().when(aiResult.summary()).thenReturn("Coding summary");
        lenient().when(aiResult.previewCsv()).thenReturn("id,name,value\n1,Alice,100");
        lenient().when(aiResult.flagged()).thenReturn(false);
        lenient().when(aiResult.flagDetails()).thenReturn(null);

        lenient().when(aiResult.piiFile().fileName()).thenReturn("test.csv");
        lenient().when(aiResult.piiFile().contentType()).thenReturn("text/csv");
        lenient().when(aiResult.piiFile().contentBase64()).thenReturn(base64Content);
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

        ReflectionTestUtils.setField(user, "id", id);

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

        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "previewCsv", "id,name,value\n1,Alice,100");
        ReflectionTestUtils.setField(post, "aiFlagged", false);
        ReflectionTestUtils.setField(post, "aiFlagDetails", null);

        return post;
    }
}