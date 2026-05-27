package com.yelos.backend.datapost.services;

import com.yelos.backend.datapost.dto.DataPostPreviewResponse;
import com.yelos.backend.datapost.dto.DataPostResponse;
import com.yelos.backend.datapost.dto.PostRequest;
import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.repo.DataPostRepo;
import com.yelos.backend.globaldto.PageResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
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

    @InjectMocks
    private DataPostService dataPostService;

    @Nested
    class PostDataPostsTests {

        @Test
        void postDataPosts_validRequest_savesPostAndReturnsResponse() {
            Long userId = 1L;

            AppUser seller = user(
                    userId,
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            );

            PostRequest request = new PostRequest(
                    userId,
                    "Coding Dataset",
                    "Full coding content",
                    "Coding summary",
                    BigDecimal.valueOf(30),
                    Genre.CODING
            );

            when(userRepo.findById(userId)).thenReturn(Optional.of(seller));

            when(dataPostRepo.save(any(DataPost.class))).thenAnswer(invocation -> {
                DataPost savedPost = invocation.getArgument(0);
                ReflectionTestUtils.setField(savedPost, "id", 10L);
                return savedPost;
            });

            DataPostResponse response = dataPostService.postDataPosts(request);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.username()).isEqualTo("seller1");
            assertThat(response.title()).isEqualTo("Coding Dataset");
            assertThat(response.summary()).isEqualTo("Coding summary");
            assertThat(response.content()).isEqualTo("Full coding content");
            assertThat(response.genre()).isEqualTo(Genre.CODING);
            assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(30));
            assertThat(response.createdAt()).isNotNull();

            verify(userRepo).findById(userId);

            verify(dataPostRepo).save(argThat(post ->
                    post.getUser().equals(seller)
                            && post.getGenre() == Genre.CODING
                            && post.getTitle().equals("Coding Dataset")
                            && post.getContent().equals("Full coding content")
                            && post.getSummary().equals("Coding summary")
                            && post.getPrice().compareTo(BigDecimal.valueOf(30)) == 0
                            && post.getStatus() == DataPostStatus.ACTIVE
            ));
        }

        @Test
        void postDataPosts_userNotFound_throwsException() {
            Long userId = 1L;

            PostRequest request = new PostRequest(
                    userId,
                    "Coding Dataset",
                    "Full coding content",
                    "Coding summary",
                    BigDecimal.valueOf(30),
                    Genre.CODING
            );

            when(userRepo.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dataPostService.postDataPosts(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User does not exists");

            verify(userRepo).findById(userId);
            verifyNoInteractions(dataPostRepo);
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
                    "Full coding content",
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
            assertThat(response.content()).isEqualTo("Full coding content");
            assertThat(response.genre()).isEqualTo(Genre.CODING);
            assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(30));

            verify(dataPostRepo).findById(10L);
            verifyNoInteractions(userRepo);
        }

        @Test
        void getDataPostById_missingPost_throwsException() {
            when(dataPostRepo.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dataPostService.getDataPostById(10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Data post not found");

            verify(dataPostRepo).findById(10L);
            verifyNoInteractions(userRepo);
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
                    "Full coding content",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            Page<DataPost> pageResult = new PageImpl<>(List.of(post));

            when(dataPostRepo.findAll(any(Pageable.class))).thenReturn(pageResult);

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

            verify(dataPostRepo).findAll(
                    argThat((Pageable pageable) ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 12
                                    && pageable.getSort().getOrderFor("createdAt") != null
                                    && pageable.getSort().getOrderFor("createdAt").isDescending()
                    )
            );

            verify(dataPostRepo, never()).findByGenre(any(), any(Pageable.class));
            verifyNoInteractions(userRepo);
        }

        @Test
        void getDataPost_specificGenre_usesFindByGenre() {
            AppUser seller = user(1L, "seller1", Role.SELLER, BigDecimal.ZERO);

            DataPost post = post(
                    10L,
                    seller,
                    Genre.FINANCE,
                    "Finance Dataset",
                    "Finance content",
                    "Finance summary",
                    BigDecimal.valueOf(40)
            );

            Page<DataPost> pageResult = new PageImpl<>(List.of(post));

            when(dataPostRepo.findByGenre(eq(Genre.FINANCE), any(Pageable.class)))
                    .thenReturn(pageResult);

            PageResponse<DataPostPreviewResponse> response = dataPostService.getDataPost(
                    0,
                    12,
                    Genre.FINANCE,
                    "priceAsc"
            );

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).genre()).isEqualTo(Genre.FINANCE);
            assertThat(response.content().get(0).title()).isEqualTo("Finance Dataset");

            verify(dataPostRepo).findByGenre(
                    eq(Genre.FINANCE),
                    argThat(pageable ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 12
                                    && pageable.getSort().getOrderFor("price") != null
                                    && pageable.getSort().getOrderFor("price").isAscending()
                    )
            );

            verify(dataPostRepo, never()).findAll(any(Pageable.class));
            verifyNoInteractions(userRepo);
        }

        @Test
        void getDataPost_invalidPageAndSize_normalizesValues() {
            Page<DataPost> emptyPage = new PageImpl<>(List.of());

            when(dataPostRepo.findAll(any(Pageable.class))).thenReturn(emptyPage);

            PageResponse<DataPostPreviewResponse> response = dataPostService.getDataPost(
                    -5,
                    999,
                    null,
                    "newest"
            );

            assertThat(response.content()).isEmpty();

            verify(dataPostRepo).findAll(argThat((Pageable pageable) ->
                    pageable.getPageNumber() == 0
                            && pageable.getPageSize() == 12
            ));
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
                    "Full coding content",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            Page<DataPost> pageResult = new PageImpl<>(List.of(post));

            when(userRepo.findById(userId)).thenReturn(Optional.of(seller));
            when(dataPostRepo.findByUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(pageResult);

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

            verify(userRepo).findById(userId);

            verify(dataPostRepo).findByUserId(
                    eq(userId),
                    argThat(pageable ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 12
                                    && pageable.getSort().getOrderFor("createdAt") != null
                                    && pageable.getSort().getOrderFor("createdAt").isAscending()
                    )
            );
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
        }
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
            String content,
            String summary,
            BigDecimal price
    ) {
        DataPost post = new DataPost(
                user,
                genre,
                title,
                content,
                summary,
                price,
                DataPostStatus.ACTIVE,
                Instant.now()
        );

        ReflectionTestUtils.setField(post, "id", id);

        return post;
    }
}