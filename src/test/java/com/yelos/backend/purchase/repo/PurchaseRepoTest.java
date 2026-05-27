package com.yelos.backend.purchase.repo;

import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.repo.DataPostRepo;
import com.yelos.backend.purchase.models.Purchase;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import com.yelos.backend.user.repo.UserRepo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PurchaseRepoTest {

    @Autowired
    private PurchaseRepo purchaseRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private DataPostRepo dataPostRepo;

    @Nested
    class ExistsByBuyerIdAndPostIdTests {

        @Test
        void existsByBuyerIdAndPostId_existingPurchase_returnsTrue() {
            AppUser buyer = userRepo.save(user(
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            ));

            AppUser seller = userRepo.save(user(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            ));

            DataPost post = dataPostRepo.save(post(
                    seller,
                    "Coding Dataset",
                    "Coding Summary",
                    "Coding Content",
                    BigDecimal.valueOf(30),
                    Genre.CODING
            ));

            Purchase purchase = purchase(
                    buyer,
                    seller,
                    post,
                    BigDecimal.valueOf(30),
                    "Coding Dataset",
                    "Coding Summary",
                    "Coding Content",
                    Genre.CODING
            );

            purchaseRepo.save(purchase);

            boolean exists = purchaseRepo.existsByBuyerIdAndPostId(
                    buyer.getId(),
                    post.getId()
            );

            assertThat(exists).isTrue();
        }

        @Test
        void existsByBuyerIdAndPostId_missingPurchase_returnsFalse() {
            AppUser buyer = userRepo.save(user(
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            ));

            AppUser seller = userRepo.save(user(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            ));

            DataPost post = dataPostRepo.save(post(
                    seller,
                    "Coding Dataset",
                    "Coding Summary",
                    "Coding Content",
                    BigDecimal.valueOf(30),
                    Genre.CODING
            ));

            boolean exists = purchaseRepo.existsByBuyerIdAndPostId(
                    buyer.getId(),
                    post.getId()
            );

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class FindByBuyerIdTests {

        @Test
        void findByBuyerId_returnsOnlyPurchasesByThatBuyer() {
            AppUser buyer1 = userRepo.save(user(
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            ));

            AppUser buyer2 = userRepo.save(user(
                    "buyer2",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            ));

            AppUser seller = userRepo.save(user(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            ));

            DataPost codingPost = dataPostRepo.save(post(
                    seller,
                    "Coding Dataset",
                    "Coding Summary",
                    "Coding Content",
                    BigDecimal.valueOf(30),
                    Genre.CODING
            ));

            DataPost financePost = dataPostRepo.save(post(
                    seller,
                    "Finance Dataset",
                    "Finance Summary",
                    "Finance Content",
                    BigDecimal.valueOf(40),
                    Genre.FINANCE
            ));

            purchaseRepo.save(purchase(
                    buyer1,
                    seller,
                    codingPost,
                    BigDecimal.valueOf(30),
                    "Coding Dataset",
                    "Coding Summary",
                    "Coding Content",
                    Genre.CODING
            ));

            purchaseRepo.save(purchase(
                    buyer2,
                    seller,
                    financePost,
                    BigDecimal.valueOf(40),
                    "Finance Dataset",
                    "Finance Summary",
                    "Finance Content",
                    Genre.FINANCE
            ));

            List<Purchase> result = purchaseRepo.findByBuyerId(buyer1.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBuyer().getId()).isEqualTo(buyer1.getId());
            assertThat(result.get(0).getTitleSnapshot()).isEqualTo("Coding Dataset");
        }

        @Test
        void findByBuyerId_userWithNoPurchases_returnsEmptyList() {
            AppUser buyer = userRepo.save(user(
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            ));

            List<Purchase> result = purchaseRepo.findByBuyerId(buyer.getId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByBuyerIdAndGenreTests {

        @Test
        void findByBuyerIdAndGenre_nullGenre_returnsAllPurchasesByBuyer() {
            AppUser buyer = userRepo.save(user(
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            ));

            AppUser seller = userRepo.save(user(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            ));

            DataPost codingPost = dataPostRepo.save(post(
                    seller,
                    "Coding Dataset",
                    "Coding Summary",
                    "Coding Content",
                    BigDecimal.valueOf(30),
                    Genre.CODING
            ));

            DataPost financePost = dataPostRepo.save(post(
                    seller,
                    "Finance Dataset",
                    "Finance Summary",
                    "Finance Content",
                    BigDecimal.valueOf(40),
                    Genre.FINANCE
            ));

            purchaseRepo.save(purchase(
                    buyer,
                    seller,
                    codingPost,
                    BigDecimal.valueOf(30),
                    "Coding Dataset",
                    "Coding Summary",
                    "Coding Content",
                    Genre.CODING
            ));

            purchaseRepo.save(purchase(
                    buyer,
                    seller,
                    financePost,
                    BigDecimal.valueOf(40),
                    "Finance Dataset",
                    "Finance Summary",
                    "Finance Content",
                    Genre.FINANCE
            ));

            Page<Purchase> result = purchaseRepo.findByBuyerIdAndGenre(
                    buyer.getId(),
                    null,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        void findByBuyerIdAndGenre_specificGenre_returnsOnlyMatchingGenre() {
            AppUser buyer = userRepo.save(user(
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            ));

            AppUser seller = userRepo.save(user(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            ));

            DataPost codingPost = dataPostRepo.save(post(
                    seller,
                    "Coding Dataset",
                    "Coding Summary",
                    "Coding Content",
                    BigDecimal.valueOf(30),
                    Genre.CODING
            ));

            DataPost financePost = dataPostRepo.save(post(
                    seller,
                    "Finance Dataset",
                    "Finance Summary",
                    "Finance Content",
                    BigDecimal.valueOf(40),
                    Genre.FINANCE
            ));

            purchaseRepo.save(purchase(
                    buyer,
                    seller,
                    codingPost,
                    BigDecimal.valueOf(30),
                    "Coding Dataset",
                    "Coding Summary",
                    "Coding Content",
                    Genre.CODING
            ));

            purchaseRepo.save(purchase(
                    buyer,
                    seller,
                    financePost,
                    BigDecimal.valueOf(40),
                    "Finance Dataset",
                    "Finance Summary",
                    "Finance Content",
                    Genre.FINANCE
            ));

            Page<Purchase> result = purchaseRepo.findByBuyerIdAndGenre(
                    buyer.getId(),
                    Genre.CODING,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getGenre()).isEqualTo(Genre.CODING);
            assertThat(result.getContent().get(0).getTitleSnapshot()).isEqualTo("Coding Dataset");
        }

        @Test
        void findByBuyerIdAndGenre_specificGenre_doesNotReturnOtherBuyerPurchases() {
            AppUser buyer1 = userRepo.save(user(
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            ));

            AppUser buyer2 = userRepo.save(user(
                    "buyer2",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            ));

            AppUser seller = userRepo.save(user(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            ));

            DataPost buyer1Post = dataPostRepo.save(post(
                    seller,
                    "Buyer 1 Coding Dataset",
                    "Summary",
                    "Content",
                    BigDecimal.valueOf(30),
                    Genre.CODING
            ));

            DataPost buyer2Post = dataPostRepo.save(post(
                    seller,
                    "Buyer 2 Coding Dataset",
                    "Summary",
                    "Content",
                    BigDecimal.valueOf(40),
                    Genre.CODING
            ));

            purchaseRepo.save(purchase(
                    buyer1,
                    seller,
                    buyer1Post,
                    BigDecimal.valueOf(30),
                    "Buyer 1 Coding Dataset",
                    "Summary",
                    "Content",
                    Genre.CODING
            ));

            purchaseRepo.save(purchase(
                    buyer2,
                    seller,
                    buyer2Post,
                    BigDecimal.valueOf(40),
                    "Buyer 2 Coding Dataset",
                    "Summary",
                    "Content",
                    Genre.CODING
            ));

            Page<Purchase> result = purchaseRepo.findByBuyerIdAndGenre(
                    buyer1.getId(),
                    Genre.CODING,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getBuyer().getId()).isEqualTo(buyer1.getId());
            assertThat(result.getContent().get(0).getTitleSnapshot()).isEqualTo("Buyer 1 Coding Dataset");
        }

        @Test
        void findByBuyerIdAndGenre_pageSizeOne_returnsOnlyOneItemInContent() {
            AppUser buyer = userRepo.save(user(
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            ));

            AppUser seller = userRepo.save(user(
                    "seller1",
                    Role.SELLER,
                    BigDecimal.ZERO
            ));

            DataPost post1 = dataPostRepo.save(post(
                    seller,
                    "Coding Dataset 1",
                    "Summary 1",
                    "Content 1",
                    BigDecimal.valueOf(30),
                    Genre.CODING
            ));

            DataPost post2 = dataPostRepo.save(post(
                    seller,
                    "Coding Dataset 2",
                    "Summary 2",
                    "Content 2",
                    BigDecimal.valueOf(40),
                    Genre.CODING
            ));

            purchaseRepo.save(purchase(
                    buyer,
                    seller,
                    post1,
                    BigDecimal.valueOf(30),
                    "Coding Dataset 1",
                    "Summary 1",
                    "Content 1",
                    Genre.CODING
            ));

            purchaseRepo.save(purchase(
                    buyer,
                    seller,
                    post2,
                    BigDecimal.valueOf(40),
                    "Coding Dataset 2",
                    "Summary 2",
                    "Content 2",
                    Genre.CODING
            ));

            Page<Purchase> result = purchaseRepo.findByBuyerIdAndGenre(
                    buyer.getId(),
                    Genre.CODING,
                    PageRequest.of(0, 1)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }
    }

    private AppUser user(
            String username,
            Role role,
            BigDecimal credits
    ) {
        AppUser user = BeanUtils.instantiateClass(AppUser.class);

        ReflectionTestUtils.setField(user, "username", username);
        ReflectionTestUtils.setField(user, "email", username + "@example.com");
        ReflectionTestUtils.setField(user, "passwordHash", "password123");
        ReflectionTestUtils.setField(user, "role", role);
        ReflectionTestUtils.setField(user, "credits", credits);
        ReflectionTestUtils.setField(user, "createdAt", Instant.now());
        ReflectionTestUtils.setField(user, "updatedAt", Instant.now());

        return user;
    }

    private DataPost post(
            AppUser seller,
            String title,
            String summary,
            String content,
            BigDecimal price,
            Genre genre
    ) {
        DataPost post = BeanUtils.instantiateClass(DataPost.class);

        ReflectionTestUtils.setField(post, "user", seller);
        ReflectionTestUtils.setField(post, "title", title);
        ReflectionTestUtils.setField(post, "summary", summary);
        ReflectionTestUtils.setField(post, "content", content);
        ReflectionTestUtils.setField(post, "price", price);
        ReflectionTestUtils.setField(post, "genre", genre);
        ReflectionTestUtils.setField(post, "createdAt", Instant.now());

        /*
         * If your DataPost.status is nullable, this line is not needed.
         * If your DataPost.status is NOT nullable, uncomment and change
         * "PUBLISHED" to the correct enum/value used in your project.
         *
         * ReflectionTestUtils.setField(post, "status", DataPostStatus.PUBLISHED);
         */

        return post;
    }

    private Purchase purchase(
            AppUser buyer,
            AppUser seller,
            DataPost post,
            BigDecimal price,
            String titleSnapshot,
            String summarySnapshot,
            String contentSnapshot,
            Genre genre
    ) {
        Purchase purchase = new Purchase(
                buyer,
                seller,
                post,
                price,
                titleSnapshot,
                summarySnapshot,
                contentSnapshot,
                Instant.now()
        );

        ReflectionTestUtils.setField(purchase, "genre", genre);

        return purchase;
    }
}