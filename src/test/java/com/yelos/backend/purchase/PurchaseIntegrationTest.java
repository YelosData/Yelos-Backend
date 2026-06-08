package com.yelos.backend.purchase;

import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.IntegrationTestBase;
import com.yelos.backend.purchase.models.Purchase;
import com.yelos.backend.storage.services.StorageDownloadService;
import com.yelos.backend.transaction.models.Transaction;
import com.yelos.backend.transaction.models.TransactionType;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import com.yelos.backend.security.AuthUserPrincipal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PurchaseIntegrationTest extends IntegrationTestBase {

    @MockitoBean
    private StorageDownloadService storageDownloadService;

    @Nested
    class PurchasePostTests {

        @Test
        void purchasePost_validPurchase_updatesCreditsSavesPurchaseAndTransactions() throws Exception {
            AppUser buyer = saveUser("buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = saveUser("seller1", Role.SELLER, BigDecimal.ZERO);

            DataPost post = savePost(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            mockMvc.perform(post("/api/purchase")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "buyerId": %d,
                                      "postId": %d
                                    }
                                    """.formatted(buyer.getId(), post.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.buyerId").value(buyer.getId()))
                    .andExpect(jsonPath("$.sellerId").value(seller.getId()))
                    .andExpect(jsonPath("$.postId").value(post.getId()))
                    .andExpect(jsonPath("$.price").value(30))
                    .andExpect(jsonPath("$.buyerCredits").value(70))
                    .andExpect(jsonPath("$.genre").value("CODING"))
                    .andExpect(jsonPath("$.titleSnapshot").value("Coding Dataset"))
                    .andExpect(jsonPath("$.summarySnapshot").value("Coding summary"))
                    .andExpect(jsonPath("$.fileNameSnapshot").value("test.csv"))
                    .andExpect(jsonPath("$.fileTypeSnapshot").value("text/csv"))
                    .andExpect(jsonPath("$.fileSizeSnapshot").value(36))
                    .andExpect(jsonPath("$.purchasedAt").exists());

            AppUser updatedBuyer = userRepo.findById(buyer.getId()).orElseThrow();
            AppUser updatedSeller = userRepo.findById(seller.getId()).orElseThrow();

            assertThat(updatedBuyer.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(70));

            assertThat(updatedSeller.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(30));

            List<Purchase> purchases = purchaseRepo.findByBuyerId(buyer.getId());

            assertThat(purchases).hasSize(1);

            Purchase savedPurchase = purchases.getFirst();

            assertThat(savedPurchase.getBuyer().getId()).isEqualTo(buyer.getId());
            assertThat(savedPurchase.getSeller().getId()).isEqualTo(seller.getId());
            assertThat(savedPurchase.getDataPost().getId()).isEqualTo(post.getId());
            assertThat(savedPurchase.getPricePaid()).isEqualByComparingTo(BigDecimal.valueOf(30));
            assertThat(savedPurchase.getTitleSnapshot()).isEqualTo("Coding Dataset");
            assertThat(savedPurchase.getSummarySnapshot()).isEqualTo("Coding summary");

            List<Transaction> transactions = transactionRepo.findAll();

            assertThat(transactions).hasSize(2);

            assertThat(transactions)
                    .anyMatch(transaction ->
                            getField(transaction, "type") == TransactionType.PURCHASE_DEBIT
                                    && userIdOf(transaction).equals(buyer.getId())
                                    && bigDecimalEquals(getField(transaction, "amount"), BigDecimal.valueOf(30))
                                    && bigDecimalEquals(getField(transaction, "balanceAfter"), BigDecimal.valueOf(70))
                    );

            assertThat(transactions)
                    .anyMatch(transaction ->
                            getField(transaction, "type") == TransactionType.SALE_CREDIT
                                    && userIdOf(transaction).equals(seller.getId())
                                    && bigDecimalEquals(getField(transaction, "amount"), BigDecimal.valueOf(30))
                                    && bigDecimalEquals(getField(transaction, "balanceAfter"), BigDecimal.valueOf(30))
                    );
        }

        @Test
        void purchasePost_insufficientCredits_doesNotSavePurchaseOrTransactions() throws Exception {
            AppUser buyer = saveUser("buyer1", Role.BUSINESS, BigDecimal.valueOf(20));
            AppUser seller = saveUser("seller1", Role.SELLER, BigDecimal.ZERO);

            DataPost post = savePost(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            mockMvc.perform(post("/api/purchase")
                            .contentType(APPLICATION_JSON)
                            .content("""
                {
                  "buyerId": %d,
                  "postId": %d
                }
                """.formatted(buyer.getId(), post.getId())))
                    .andExpect(status().isBadRequest())
                    .andExpect(result ->
                            assertThat(result.getResponse().getErrorMessage())
                                    .isEqualTo("Insufficient credits")
                    );

            AppUser updatedBuyer = userRepo.findById(buyer.getId()).orElseThrow();
            AppUser updatedSeller = userRepo.findById(seller.getId()).orElseThrow();

            assertThat(updatedBuyer.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(20));

            assertThat(updatedSeller.getCredits())
                    .isEqualByComparingTo(BigDecimal.ZERO);

            assertThat(purchaseRepo.findAll()).isEmpty();
            assertThat(transactionRepo.findAll()).isEmpty();
        }

        @Test
        void purchasePost_alreadyPurchased_doesNotCreateSecondPurchase() throws Exception {
            AppUser buyer = saveUser("buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = saveUser("seller1", Role.SELLER, BigDecimal.ZERO);

            DataPost post = savePost(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            mockMvc.perform(post("/api/purchase")
                            .contentType(APPLICATION_JSON)
                            .content("""
                    {
                      "buyerId": %d,
                      "postId": %d
                    }
                    """.formatted(buyer.getId(), post.getId())))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/purchase")
                            .contentType(APPLICATION_JSON)
                            .content("""
                    {
                      "buyerId": %d,
                      "postId": %d
                    }
                    """.formatted(buyer.getId(), post.getId())))
                    .andExpect(status().isConflict())
                    .andExpect(result ->
                            assertThat(result.getResponse().getErrorMessage())
                                    .isEqualTo("You have already purchased this post")
                    );

            AppUser updatedBuyer = userRepo.findById(buyer.getId()).orElseThrow();
            AppUser updatedSeller = userRepo.findById(seller.getId()).orElseThrow();

            assertThat(updatedBuyer.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(70));

            assertThat(updatedSeller.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(30));

            assertThat(purchaseRepo.findByBuyerId(buyer.getId()))
                    .hasSize(1);

            assertThat(transactionRepo.findAll())
                    .hasSize(2);
        }

        @Test
        void purchasePost_buyerCannotPurchaseOwnPost() throws Exception {
            AppUser user = saveUser("buyerSeller1", Role.BUSINESS, BigDecimal.valueOf(100));

            DataPost post = savePost(
                    user,
                    Genre.CODING,
                    "Own Dataset",
                    "Own summary",
                    BigDecimal.valueOf(30)
            );

            mockMvc.perform(post("/api/purchase")
                            .contentType(APPLICATION_JSON)
                            .content("""
                    {
                      "buyerId": %d,
                      "postId": %d
                    }
                    """.formatted(user.getId(), post.getId())))
                    .andExpect(status().isBadRequest())
                    .andExpect(result ->
                            assertThat(result.getResponse().getErrorMessage())
                                    .isEqualTo("You cannot purchase your own post")
                    );

            AppUser updatedUser = userRepo.findById(user.getId()).orElseThrow();

            assertThat(updatedUser.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(100));

            assertThat(purchaseRepo.findAll()).isEmpty();
            assertThat(transactionRepo.findAll()).isEmpty();
        }

        @Test
        void purchasePost_nonBusinessUserCannotPurchase() throws Exception {
            AppUser buyer = saveUser("sellerTryingToBuy", Role.SELLER, BigDecimal.valueOf(100));
            AppUser seller = saveUser("seller1", Role.SELLER, BigDecimal.ZERO);

            DataPost post = savePost(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            mockMvc.perform(post("/api/purchase")
                            .contentType(APPLICATION_JSON)
                            .content("""
                {
                  "buyerId": %d,
                  "postId": %d
                }
                """.formatted(buyer.getId(), post.getId())))
                    .andExpect(status().isForbidden())
                    .andExpect(result ->
                            assertThat(result.getResponse().getErrorMessage())
                                    .isEqualTo("Only business users can purchase posts")
                    );

            assertThat(purchaseRepo.findAll()).isEmpty();
            assertThat(transactionRepo.findAll()).isEmpty();
        }
    }

    @Nested
    class PurchaseHistoryTests {

        @Test
        void getPurchaseHistoryByUserId_returnsPagedPurchaseHistory() throws Exception {
            AppUser buyer = saveUser("buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = saveUser("seller1", Role.SELLER, BigDecimal.ZERO);

            DataPost post = savePost(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            mockMvc.perform(post("/api/purchase")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "buyerId": %d,
                                      "postId": %d
                                    }
                                    """.formatted(buyer.getId(), post.getId())))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/purchase/users/{userId}/history", buyer.getId())
                            .param("page", "0")
                            .param("size", "6")
                            .param("genre", "CODING")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].buyerId").value(buyer.getId()))
                    .andExpect(jsonPath("$.content[0].sellerId").value(seller.getId()))
                    .andExpect(jsonPath("$.content[0].postId").value(post.getId()))
                    .andExpect(jsonPath("$.content[0].genre").value("CODING"))
                    .andExpect(jsonPath("$.content[0].titleSnapshot").value("Coding Dataset"))
                    .andExpect(jsonPath("$.content[0].summarySnapshot").value("Coding summary"))
                    .andExpect(jsonPath("$.content[0].fileNameSnapshot").value("test.csv"))
                    .andExpect(jsonPath("$.content[0].fileTypeSnapshot").value("text/csv"))
                    .andExpect(jsonPath("$.content[0].fileSizeSnapshot").value(36))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(6))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        void getPurchasesByUserId_returnsPurchaseList() throws Exception {
            AppUser buyer = saveUser("buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = saveUser("seller1", Role.SELLER, BigDecimal.ZERO);

            DataPost post = savePost(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );

            mockMvc.perform(post("/api/purchase")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "buyerId": %d,
                                      "postId": %d
                                    }
                                    """.formatted(buyer.getId(), post.getId())))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/purchase/users/{userId}/posts", buyer.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].buyerId").value(buyer.getId()))
                    .andExpect(jsonPath("$[0].sellerId").value(seller.getId()))
                    .andExpect(jsonPath("$[0].postId").value(post.getId()))
                    .andExpect(jsonPath("$[0].genre").value("CODING"))
                    .andExpect(jsonPath("$[0].titleSnapshot").value("Coding Dataset"))
                    .andExpect(jsonPath("$[0].summarySnapshot").value("Coding summary"))
                    .andExpect(jsonPath("$[0].fileNameSnapshot").value("test.csv"))
                    .andExpect(jsonPath("$[0].fileTypeSnapshot").value("text/csv"))
                    .andExpect(jsonPath("$[0].fileSizeSnapshot").value(36));
        }
    }

    @Nested
    class PurchaseDownloadTests {
        @Test
        void downloadPurchasedFile_validBuyer_returnsSignedDownloadResponse() throws Exception {
            AppUser buyer = saveUser("buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = saveUser("seller1", Role.SELLER, BigDecimal.ZERO);
            DataPost post = savePost(
                    seller,
                    Genre.CODING,
                    "Coding Dataset",
                    "Coding summary",
                    BigDecimal.valueOf(30)
            );
            mockMvc.perform(post("/api/purchase")
                    .contentType(APPLICATION_JSON)
                    .content(""" 
                            { "buyerId": %d, "postId": %d }"""
                            .formatted(buyer.getId(), post.getId())))
                    .andExpect(status().isOk());
            Purchase savedPurchase = purchaseRepo.findByBuyerId(buyer.getId()).getFirst();
            when(storageDownloadService.generateDownloadUrl(
                    savedPurchase.getBucketNameSnapshot(),
                    savedPurchase.getObjectKeySnapshot(),
                    savedPurchase.getFileNameSnapshot()))
                    .thenReturn("https://signed-url.example.com");
            AuthUserPrincipal principal = new AuthUserPrincipal(
                    buyer.getId(),
                    buyer.getUsername(),
                    "BUSINESS"
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_BUSINESS")));
            try {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                mockMvc.perform(get("/api/purchase/{purchaseId}/download", savedPurchase.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.downloadUrl").value("https://signed-url.example.com"))
                        .andExpect(jsonPath("$.fileName").value("test.csv"))
                        .andExpect(jsonPath("$.fileType").value("text/csv"))
                        .andExpect(jsonPath("$.fileSize").value(36))
                        .andExpect(jsonPath("$.expiresInSeconds").value(300));
            } finally {
                SecurityContextHolder.clearContext();
            }
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
        DataPost post = new DataPost();
        ReflectionTestUtils.setField(post, "user", seller);
        ReflectionTestUtils.setField(post, "genre", genre);
        ReflectionTestUtils.setField(post, "title", title);
        ReflectionTestUtils.setField(post, "summary", summary);
        ReflectionTestUtils.setField(post, "price", price);
        ReflectionTestUtils.setField(post, "status", DataPostStatus.ACTIVE);
        ReflectionTestUtils.setField(post, "createdAt", Instant.now());
        ReflectionTestUtils.setField(post, "fileName", "test.csv");
        ReflectionTestUtils.setField(post, "fileType", "text/csv");
        ReflectionTestUtils.setField(post, "fileSize", 36L);
        ReflectionTestUtils.setField(post, "bucketName", "yelos-datasets");
        ReflectionTestUtils.setField( post, "objectKey",  "sellers/" + seller.getId() + "/posts/" + UUID.randomUUID() + "-test.csv");
        try {
            ReflectionTestUtils.setField(post, "content", null);
        } catch (IllegalArgumentException ignored) {}

        return dataPostRepo.save(post);
    }

    private Object getField(Object target, String fieldName) {
        return ReflectionTestUtils.getField(target, fieldName);
    }

    private Long userIdOf(Transaction transaction) {
        AppUser user = (AppUser) getField(transaction, "user");
        return user.getId();
    }

    private boolean bigDecimalEquals(Object actual, BigDecimal expected) {
        if (!(actual instanceof BigDecimal actualBigDecimal)) {
            return false;
        }

        return actualBigDecimal.compareTo(expected) == 0;
    }
}