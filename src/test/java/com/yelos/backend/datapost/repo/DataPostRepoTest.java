package com.yelos.backend.datapost.repo;

import com.yelos.backend.IntegrationTestBase;
import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class DataPostRepoTest extends IntegrationTestBase {

    @Nested
    class FindByUserIdTests {

        @Test
        void findByUserId_returnsOnlyPostsByThatUser() {
            AppUser seller1 = userRepo.save(user("seller1", Role.SELLER));
            AppUser seller2 = userRepo.save(user("seller2", Role.SELLER));

            dataPostRepo.save(post(
                    seller1,
                    Genre.CODING,
                    "Seller 1 Dataset",
                    "Summary 1",
                    BigDecimal.valueOf(30),
                    DataPostStatus.ACTIVE
            ));

            dataPostRepo.save(post(
                    seller2,
                    Genre.FINANCE,
                    "Seller 2 Dataset",
                    "Summary 2",
                    BigDecimal.valueOf(40),
                    DataPostStatus.ACTIVE
            ));

            Page<DataPost> result = dataPostRepo.findByUserId(
                    seller1.getId(),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUser().getId()).isEqualTo(seller1.getId());
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Seller 1 Dataset");
            assertThat(result.getContent().get(0).getFileName()).isEqualTo("test.csv");
            assertThat(result.getContent().get(0).getObjectKey()).isNotBlank();
            assertThat(result.getContent().get(0).getEmbedding()).containsExactly(0.1f, 0.2f, 0.3f);
        }

        @Test
        void findByUserId_userWithNoPosts_returnsEmptyPage() {
            AppUser seller = userRepo.save(user("seller1", Role.SELLER));

            Page<DataPost> result = dataPostRepo.findByUserId(
                    seller.getId(),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    class FindByGenreTests {

        @Test
        void findByGenre_returnsOnlyMatchingGenre() {
            AppUser seller = userRepo.save(user("seller1", Role.SELLER));

            dataPostRepo.save(post(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding Summary",
                    BigDecimal.valueOf(30),
                    DataPostStatus.ACTIVE
            ));

            dataPostRepo.save(post(
                    seller,
                    Genre.FINANCE,
                    "Finance Dataset",
                    "Finance Summary",
                    BigDecimal.valueOf(40),
                    DataPostStatus.ACTIVE
            ));

            Page<DataPost> result = dataPostRepo.findByGenre(
                    Genre.CODING,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getGenre()).isEqualTo(Genre.CODING);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Coding Dataset");
            assertThat(result.getContent().get(0).getFileName()).isEqualTo("test.csv");
            assertThat(result.getContent().get(0).getBucketName()).isEqualTo("yelos-datasets");
            assertThat(result.getContent().get(0).getEmbedding()).containsExactly(0.1f, 0.2f, 0.3f);
        }

        @Test
        void findByGenre_noMatchingGenre_returnsEmptyPage() {
            AppUser seller = userRepo.save(user("seller1", Role.SELLER));

            dataPostRepo.save(post(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding Summary",
                    BigDecimal.valueOf(30),
                    DataPostStatus.ACTIVE
            ));

            Page<DataPost> result = dataPostRepo.findByGenre(
                    Genre.FINANCE,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    class MarketplaceStatusTests {

        @Test
        void findByStatus_returnsOnlyActivePosts() {
            AppUser seller = userRepo.save(user("seller1", Role.SELLER));

            dataPostRepo.save(post(
                    seller,
                    Genre.CODING,
                    "Active Dataset",
                    "Active Summary",
                    BigDecimal.valueOf(30),
                    DataPostStatus.ACTIVE
            ));

            dataPostRepo.save(post(
                    seller,
                    Genre.FINANCE,
                    "Deleted Dataset",
                    "Deleted Summary",
                    BigDecimal.valueOf(40),
                    DataPostStatus.DELETED
            ));

            Page<DataPost> result = dataPostRepo.findByStatus(
                    DataPostStatus.ACTIVE,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Active Dataset");
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(DataPostStatus.ACTIVE);
            assertThat(result.getContent().get(0).getEmbedding()).containsExactly(0.1f, 0.2f, 0.3f);
        }

        @Test
        void findByGenreAndStatus_returnsOnlyMatchingActiveGenre() {
            AppUser seller = userRepo.save(user("seller1", Role.SELLER));

            dataPostRepo.save(post(
                    seller,
                    Genre.CODING,
                    "Active Coding Dataset",
                    "Coding Summary",
                    BigDecimal.valueOf(30),
                    DataPostStatus.ACTIVE
            ));

            dataPostRepo.save(post(
                    seller,
                    Genre.CODING,
                    "Deleted Coding Dataset",
                    "Deleted Coding Summary",
                    BigDecimal.valueOf(40),
                    DataPostStatus.DELETED
            ));

            dataPostRepo.save(post(
                    seller,
                    Genre.FINANCE,
                    "Active Finance Dataset",
                    "Finance Summary",
                    BigDecimal.valueOf(50),
                    DataPostStatus.ACTIVE
            ));

            Page<DataPost> result = dataPostRepo.findByGenreAndStatus(
                    Genre.CODING,
                    DataPostStatus.ACTIVE,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getGenre()).isEqualTo(Genre.CODING);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(DataPostStatus.ACTIVE);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Active Coding Dataset");
            assertThat(result.getContent().get(0).getFileName()).isEqualTo("test.csv");
            assertThat(result.getContent().get(0).getBucketName()).isEqualTo("yelos-datasets");
            assertThat(result.getContent().get(0).getEmbedding()).containsExactly(0.1f, 0.2f, 0.3f);
        }

        @Test
        void findByGenreAndStatus_noMatchingActiveGenre_returnsEmptyPage() {
            AppUser seller = userRepo.save(user("seller1", Role.SELLER));

            dataPostRepo.save(post(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding Summary",
                    BigDecimal.valueOf(30),
                    DataPostStatus.ACTIVE
            ));

            Page<DataPost> result = dataPostRepo.findByGenreAndStatus(
                    Genre.FINANCE,
                    DataPostStatus.ACTIVE,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    private AppUser user(
            String username,
            Role role
    ) {
        return new AppUser(
                username,
                username + "@example.com",
                "password123",
                BigDecimal.ZERO,
                role,
                Instant.now(),
                Instant.now()
        );
    }

    private DataPost post(
            AppUser user,
            Genre genre,
            String title,
            String summary,
            BigDecimal price,
            DataPostStatus status
    ) {
        DataPost post = new DataPost(
                user,
                genre,
                title,
                summary,
                price,
                status,
                Instant.now(),
                "test.csv",
                "text/csv",
                36L,
                "yelos-datasets",
                "sellers/" + user.getId() + "/posts/" + UUID.randomUUID() + "-test.csv"
        );

        post.setEmbedding(new float[]{0.1f, 0.2f, 0.3f});

        return post;
    }
}