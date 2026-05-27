package com.yelos.backend.datapost.repo;

import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import com.yelos.backend.user.repo.UserRepo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DataPostRepoTest {

    @Autowired
    private DataPostRepo dataPostRepo;

    @Autowired
    private UserRepo userRepo;

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
                    "Content 1",
                    "Summary 1",
                    BigDecimal.valueOf(30)
            ));

            dataPostRepo.save(post(
                    seller2,
                    Genre.FINANCE,
                    "Seller 2 Dataset",
                    "Content 2",
                    "Summary 2",
                    BigDecimal.valueOf(40)
            ));

            Page<DataPost> result = dataPostRepo.findByUserId(
                    seller1.getId(),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUser().getId()).isEqualTo(seller1.getId());
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Seller 1 Dataset");
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
                    "Coding Content",
                    "Coding Summary",
                    BigDecimal.valueOf(30)
            ));

            dataPostRepo.save(post(
                    seller,
                    Genre.FINANCE,
                    "Finance Dataset",
                    "Finance Content",
                    "Finance Summary",
                    BigDecimal.valueOf(40)
            ));

            Page<DataPost> result = dataPostRepo.findByGenre(
                    Genre.CODING,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getGenre()).isEqualTo(Genre.CODING);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Coding Dataset");
        }

        @Test
        void findByGenre_noMatchingGenre_returnsEmptyPage() {
            AppUser seller = userRepo.save(user("seller1", Role.SELLER));

            dataPostRepo.save(post(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding Content",
                    "Coding Summary",
                    BigDecimal.valueOf(30)
            ));

            Page<DataPost> result = dataPostRepo.findByGenre(
                    Genre.FINANCE,
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
            String content,
            String summary,
            BigDecimal price
    ) {
        return new DataPost(
                user,
                genre,
                title,
                content,
                summary,
                price,
                DataPostStatus.ACTIVE,
                Instant.now()
        );
    }
}