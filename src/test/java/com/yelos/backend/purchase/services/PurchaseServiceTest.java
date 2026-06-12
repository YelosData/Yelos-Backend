package com.yelos.backend.purchase.services;

import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.repo.DataPostRepo;
import com.yelos.backend.globaldto.PageResponse;
import com.yelos.backend.purchase.dto.PurchaseDownloadResponse;
import com.yelos.backend.purchase.dto.PurchasePostRequest;
import com.yelos.backend.purchase.dto.PurchaseResponse;
import com.yelos.backend.purchase.models.Purchase;
import com.yelos.backend.purchase.repo.PurchaseRepo;
import com.yelos.backend.storage.services.StorageDownloadService;
import com.yelos.backend.transaction.services.TransactionService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private DataPostRepo dataPostRepo;

    @Mock
    private PurchaseRepo purchaseRepo;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StorageDownloadService storageDownloadService;

    @InjectMocks
    private PurchaseService purchaseService;

    @Nested
    class PurchasePostTest {
            @Test
            void purchasePost_validPurchase_updatesCreditsAndSavesPurchaseWithFileSnapshot() {

                Long buyerId = 1L;
                Long sellerId = 2L;
                Long postId = 10L;

                AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
                AppUser seller = user(sellerId, "seller1", Role.SELLER, BigDecimal.ZERO);
                Genre genre = Genre.CODING;

                DataPost post = post(
                        postId,
                        seller,
                        "ChatGPT Dataset",
                        "Useful conversation data",
                        BigDecimal.valueOf(30),
                        genre
                );

                when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
                when(dataPostRepo.findById(postId)).thenReturn(Optional.of(post));
                when(purchaseRepo.existsByBuyerIdAndPostId(buyerId, postId)).thenReturn(false);

                when(purchaseRepo.save(any(Purchase.class))).thenAnswer(invocation -> {
                    Purchase savedPurchase = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedPurchase, "id", 100L);
                    return savedPurchase;
                });

                PurchaseResponse response = purchaseService.purchasePost(
                        new PurchasePostRequest(buyerId, postId)
                );

                assertThat(buyer.getCredits())
                        .isEqualByComparingTo(BigDecimal.valueOf(70));

                assertThat(seller.getCredits())
                        .isEqualByComparingTo(BigDecimal.valueOf(30));

                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo(100L);
                assertThat(response.titleSnapshot()).isEqualTo("ChatGPT Dataset");
                assertThat(response.summarySnapshot()).isEqualTo("Useful conversation data");
                assertThat(response.fileNameSnapshot()).isEqualTo("test.csv");
                assertThat(response.fileTypeSnapshot()).isEqualTo("text/csv");
                assertThat(response.fileSizeSnapshot()).isEqualTo(36L);

                verify(userRepo).findById(buyerId);
                verify(dataPostRepo).findById(postId);
                verify(purchaseRepo).existsByBuyerIdAndPostId(buyerId, postId);

                verify(userRepo).save(buyer);
                verify(userRepo).save(seller);

                verify(purchaseRepo).save(argThat(purchase ->
                        "ChatGPT Dataset".equals(getField(purchase, "titleSnapshot"))
                                && "Useful conversation data".equals(getField(purchase, "summarySnapshot"))
                                && "test.csv".equals(getField(purchase, "fileNameSnapshot"))
                                && "text/csv".equals(getField(purchase, "fileTypeSnapshot"))
                                && Long.valueOf(36L).equals(getField(purchase, "fileSizeSnapshot"))
                                && "yelos-datasets".equals(getField(purchase, "bucketNameSnapshot"))
                                && "sellers/2/posts/test-file.csv".equals(getField(purchase, "objectKeySnapshot"))
                ));

                verify(transactionService).PurchaseTransaction(
                        eq(buyer),
                        eq(seller),
                        any(Purchase.class),
                        argThat(amount -> amount.compareTo(BigDecimal.valueOf(30)) == 0)
                );

                verifyNoInteractions(storageDownloadService);
            }

            @Test
            void purchasePost_postWithoutCloudFile_throwsException() {
                Long buyerId = 1L;
                Long postId = 10L;

                AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
                AppUser seller = user(2L, "seller1", Role.SELLER, BigDecimal.ZERO);

                DataPost post = post(
                        postId,
                        seller,
                        "Legacy Dataset",
                        "Legacy summary",
                        BigDecimal.valueOf(30),
                        Genre.CODING
                );

                ReflectionTestUtils.setField(post, "objectKey", null);

                when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
                when(dataPostRepo.findById(postId)).thenReturn(Optional.of(post));
                when(purchaseRepo.existsByBuyerIdAndPostId(buyerId, postId)).thenReturn(false);

                assertThatThrownBy(() ->
                        purchaseService.purchasePost(new PurchasePostRequest(buyerId, postId))
                ).isInstanceOf(RuntimeException.class);

                assertThat(buyer.getCredits()).isEqualByComparingTo(BigDecimal.valueOf(100));
                assertThat(seller.getCredits()).isEqualByComparingTo(BigDecimal.ZERO);

                verify(purchaseRepo, never()).save(any(Purchase.class));
                verify(userRepo, never()).save(any(AppUser.class));
                verifyNoInteractions(transactionService);
                verifyNoInteractions(storageDownloadService);
            }

            @Test
            void purchasePost_buyerNotFound_throwsException() {

                Long buyerId = 1L;
                Long postId = 10L;

                when(userRepo.findById(buyerId)).thenReturn(Optional.empty());

                assertThatThrownBy(() ->
                        purchaseService.purchasePost(new PurchasePostRequest(buyerId, postId))
                ).isInstanceOf(RuntimeException.class);

                verify(userRepo).findById(buyerId);
                verifyNoInteractions(dataPostRepo);
                verifyNoInteractions(transactionService);
                verifyNoInteractions(storageDownloadService);
                verify(purchaseRepo, never()).save(any(Purchase.class));
            }

            @Test
            void purchasePost_postNotFound_throwsException() {

                Long buyerId = 1L;
                Long postId = 10L;

                AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));

                when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
                when(dataPostRepo.findById(postId)).thenReturn(Optional.empty());

                assertThatThrownBy(() ->
                        purchaseService.purchasePost(new PurchasePostRequest(buyerId, postId))
                ).isInstanceOf(RuntimeException.class);

                verify(userRepo).findById(buyerId);
                verify(dataPostRepo).findById(postId);
                verifyNoInteractions(transactionService);
                verifyNoInteractions(storageDownloadService);
                verify(purchaseRepo, never()).save(any(Purchase.class));
            }

            @Test
            void purchasePost_nonBusinessBuyer_throwsException() {

                Long buyerId = 1L;
                Long postId = 10L;

                AppUser buyer = user(buyerId, "sellerTryingToBuy", Role.SELLER, BigDecimal.valueOf(100));
                AppUser seller = user(2L, "seller1", Role.SELLER, BigDecimal.valueOf(0));
                Genre genre = Genre.CODING;

                DataPost post = post(
                        postId,
                        seller,
                        "Dataset",
                        "Summary",
                        BigDecimal.valueOf(30),
                        genre
                );

                when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
                when(dataPostRepo.findById(postId)).thenReturn(Optional.of(post));

                assertThatThrownBy(() ->
                        purchaseService.purchasePost(new PurchasePostRequest(buyerId, postId))
                ).isInstanceOf(RuntimeException.class);

                verify(purchaseRepo, never()).save(any(Purchase.class));
                verifyNoInteractions(transactionService);
                verifyNoInteractions(storageDownloadService);
                verify(userRepo, never()).save(any(AppUser.class));
            }

        @Test
        void purchasePost_sameBuyerAndSellerId_throwsException() {

            Long buyerId = 1L;
            Long postId = 10L;

            AppUser buyer = user(buyerId, "sellerTryingToBuy", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = user(buyerId, "seller1", Role.BUSINESS, BigDecimal.valueOf(0));
            Genre genre = Genre.CODING;

            DataPost post = post(
                    postId,
                    seller,
                    "Dataset",
                    "Summary",
                    BigDecimal.valueOf(30),
                    genre
            );

            when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
            when(dataPostRepo.findById(postId)).thenReturn(Optional.of(post));

            assertThatThrownBy(() ->
                    purchaseService.purchasePost(new PurchasePostRequest(buyerId, postId))
            ).isInstanceOf(RuntimeException.class);

            verify(purchaseRepo, never()).save(any(Purchase.class));
            verifyNoInteractions(transactionService);
            verifyNoInteractions(storageDownloadService);
            verify(userRepo, never()).save(any(AppUser.class));
        }

        @Test
        void purchasePost_insufficientCredits_throwsException() {
            // Arrange
            Long buyerId = 1L;
            Long postId = 10L;

            AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(20));
            AppUser seller = user(2L, "seller1", Role.SELLER, BigDecimal.ZERO);
            Genre genre = Genre.CODING;

            DataPost post = post(
                    postId,
                    seller,
                    "Dataset",
                    "Summary",
                    BigDecimal.valueOf(30),
                    genre
            );

            when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
            when(dataPostRepo.findById(postId)).thenReturn(Optional.of(post));
            when(purchaseRepo.existsByBuyerIdAndPostId(buyerId, postId)).thenReturn(false);

            assertThatThrownBy(() ->
                    purchaseService.purchasePost(new PurchasePostRequest(buyerId, postId))
            ).isInstanceOf(RuntimeException.class);

            assertThat(buyer.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(20));
            assertThat(seller.getCredits())
                    .isEqualByComparingTo(BigDecimal.ZERO);

            verify(purchaseRepo, never()).save(any(Purchase.class));
            verifyNoInteractions(transactionService);
            verifyNoInteractions(storageDownloadService);
        }

        @Test
        void purchasePost_alreadyPurchased_throwsException() {

            Long buyerId = 1L;
            Long postId = 10L;

            AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = user(2L, "seller1", Role.SELLER, BigDecimal.ZERO);
            Genre genre = Genre.CODING;

            DataPost post = post(
                    postId,
                    seller,
                    "Dataset",
                    "Summary",
                    BigDecimal.valueOf(30),
                    genre
            );

            when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
            when(dataPostRepo.findById(postId)).thenReturn(Optional.of(post));
            when(purchaseRepo.existsByBuyerIdAndPostId(buyerId, postId)).thenReturn(true);

            assertThatThrownBy(() ->
                    purchaseService.purchasePost(new PurchasePostRequest(buyerId, postId))
            ).isInstanceOf(RuntimeException.class);

            verify(purchaseRepo, never()).save(any(Purchase.class));
            verifyNoInteractions(transactionService);
            verifyNoInteractions(storageDownloadService);
        }

        @Test
        void purchasePost_exactCredits_succeeds() {

            Long buyerId = 1L;
            Long postId = 10L;

            AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(30));
            AppUser seller = user(2L, "seller1", Role.SELLER, BigDecimal.ZERO);
            Genre genre = Genre.CODING;

            DataPost post = post(
                    postId,
                    seller,
                    "Dataset",
                    "Summary",
                    BigDecimal.valueOf(30),
                    genre
            );

            when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
            when(dataPostRepo.findById(postId)).thenReturn(Optional.of(post));
            when(purchaseRepo.existsByBuyerIdAndPostId(buyerId, postId)).thenReturn(false);

            when(purchaseRepo.save(any(Purchase.class))).thenAnswer(invocation -> {
                Purchase savedPurchase = invocation.getArgument(0);
                ReflectionTestUtils.setField(savedPurchase, "id", 100L);
                return savedPurchase;
            });

            purchaseService.purchasePost(new PurchasePostRequest(buyerId, postId));

            assertThat(buyer.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(0));
            assertThat(seller.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(30));

            verify(purchaseRepo).save(any(Purchase.class));
            verify(transactionService).PurchaseTransaction(
                    eq(buyer),
                    eq(seller),
                    any(Purchase.class),
                    argThat(amount -> amount.compareTo(BigDecimal.valueOf(30)) == 0)
            );
            verifyNoInteractions(storageDownloadService);
        }
    }

    @Nested
    class GetPurchasesByUserIdTests {

        @Test
        void getPurchasesByUserId_validUser_returnsPurchaseResponses() {

            Long buyerId = 1L;
            Long sellerId = 2L;

            AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = user(sellerId, "seller1", Role.SELLER, BigDecimal.ZERO);
            Genre genre = Genre.CODING;

            DataPost post1 = post(
                    10L,
                    seller,
                    "Dataset 1",
                    "Summary 1",
                    BigDecimal.valueOf(30),
                    genre
            );

            DataPost post2 = post(
                    11L,
                    seller,
                    "Dataset 2",
                    "Summary 2",
                    BigDecimal.valueOf(40),
                    genre
            );

            Purchase purchase1 = purchase(
                    100L,
                    buyer,
                    seller,
                    post1,
                    BigDecimal.valueOf(30),
                    "Dataset 1",
                    "Summary 1"
            );

            Purchase purchase2 = purchase(
                    101L,
                    buyer,
                    seller,
                    post2,
                    BigDecimal.valueOf(40),
                    "Dataset 2",
                    "Summary 2"
            );

            when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
            when(purchaseRepo.findByBuyerId(buyerId)).thenReturn(List.of(purchase1, purchase2));

            List<PurchaseResponse> responses = purchaseService.getPurchasesByUserId(buyerId);

            assertThat(responses).hasSize(2);

            assertThat(responses.get(0).titleSnapshot()).isEqualTo("Dataset 1");
            assertThat(responses.get(0).summarySnapshot()).isEqualTo("Summary 1");
            assertThat(responses.get(0).fileNameSnapshot()).isEqualTo("test.csv");
            assertThat(responses.get(0).fileTypeSnapshot()).isEqualTo("text/csv");
            assertThat(responses.get(0).fileSizeSnapshot()).isEqualTo(36L);

            assertThat(responses.get(1).titleSnapshot()).isEqualTo("Dataset 2");
            assertThat(responses.get(1).summarySnapshot()).isEqualTo("Summary 2");
            assertThat(responses.get(1).fileNameSnapshot()).isEqualTo("test.csv");

            verify(userRepo).findById(buyerId);
            verify(purchaseRepo).findByBuyerId(buyerId);

            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(transactionService);
            verifyNoInteractions(storageDownloadService);
        }

        @Test
        void getPurchasesByUserId_userNotFound_throwsException() {

            Long userId = 1L;

            when(userRepo.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> purchaseService.getPurchasesByUserId(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User does not exist");

            verify(userRepo).findById(userId);

            verifyNoInteractions(purchaseRepo);
            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(transactionService);
            verifyNoInteractions(storageDownloadService);
        }

        @Test
        void getPurchasesByUserId_validUserWithNoPurchases_returnsEmptyList() {

            Long buyerId = 1L;

            AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));

            when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
            when(purchaseRepo.findByBuyerId(buyerId)).thenReturn(List.of());

            List<PurchaseResponse> responses = purchaseService.getPurchasesByUserId(buyerId);

            assertThat(responses).isEmpty();

            verify(userRepo).findById(buyerId);
            verify(purchaseRepo).findByBuyerId(buyerId);

            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(transactionService);
            verifyNoInteractions(storageDownloadService);
        }
    }

    @Nested
    class GetPurchaseHistoryByUserIdTests {

        @Test
        void getPurchaseHistoryByUserId_validBusinessUserWithAllGenre_returnsPagedResponse() {

            Long buyerId = 1L;
            Long sellerId = 2L;

            AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = user(sellerId, "seller1", Role.SELLER, BigDecimal.ZERO);
            Genre genre = Genre.CODING;

            DataPost post = post(
                    10L,
                    seller,
                    "Dataset 1",
                    "Summary 1",
                    BigDecimal.valueOf(30),
                    genre
            );

            Purchase purchase = purchase(
                    100L,
                    buyer,
                    seller,
                    post,
                    BigDecimal.valueOf(30),
                    "Dataset 1",
                    "Summary 1"
            );

            Page<Purchase> purchasePage = new PageImpl<>(List.of(purchase));

            when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
            when(purchaseRepo.findByBuyerIdAndGenre(
                    eq(buyerId),
                    isNull(),
                    any(Pageable.class)
            )).thenReturn(purchasePage);

            PageResponse<PurchaseResponse> response = purchaseService.getPurchaseHistoryByUserId(
                    buyerId,
                    0,
                    6,
                    "ALL",
                    "newest"
            );

            assertThat(response).isNotNull();
            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).titleSnapshot()).isEqualTo("Dataset 1");
            assertThat(response.content().get(0).fileNameSnapshot()).isEqualTo("test.csv");

            verify(userRepo).findById(buyerId);

            verify(purchaseRepo).findByBuyerIdAndGenre(
                    eq(buyerId),
                    isNull(),
                    argThat(pageable ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 6
                    )
            );

            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(transactionService);
            verifyNoInteractions(storageDownloadService);
        }


        @Test
        void getPurchaseHistoryByUserId_validBusinessUserWithSpecificGenre_returnsOnlyThatGenre() {
            Long buyerId = 1L;
            Long sellerId = 2L;

            AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = user(sellerId, "seller1", Role.SELLER, BigDecimal.ZERO);

            Genre genre1 = Genre.CODING;
            Genre genre2 = Genre.FINANCE;

            DataPost codingPost = post(
                    10L,
                    seller,
                    "Coding Dataset",
                    "Coding Summary",
                    BigDecimal.valueOf(30),
                    genre1
            );

            DataPost financePost = post(
                    11L,
                    seller,
                    "Finance Dataset",
                    "Finance Summary",
                    BigDecimal.valueOf(40),
                    genre2
            );

            Purchase codingPurchase = purchase(
                    100L,
                    buyer,
                    seller,
                    codingPost,
                    BigDecimal.valueOf(30),
                    "Coding Dataset",
                    "Coding Summary"
            );

            Purchase financePurchase = purchase(
                    101L,
                    buyer,
                    seller,
                    financePost,
                    BigDecimal.valueOf(40),
                    "Finance Dataset",
                    "Finance Summary"
            );

            Page<Purchase> codingOnlyPage = new PageImpl<>(
                    List.of(codingPurchase),
                    PageRequest.of(0, 6),
                    1
            );

            when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));

            when(purchaseRepo.findByBuyerIdAndGenre(
                    eq(buyerId),
                    eq(Genre.CODING),
                    any(Pageable.class)
            )).thenReturn(codingOnlyPage);

            PageResponse<PurchaseResponse> response = purchaseService.getPurchaseHistoryByUserId(
                    buyerId,
                    0,
                    6,
                    "coding",
                    "newest"
            );

            assertThat(response).isNotNull();
            assertThat(response.content()).hasSize(1);

            assertThat(response.content().get(0).titleSnapshot())
                    .isEqualTo("Coding Dataset");

            assertThat(response.content())
                    .noneMatch(purchaseResponse ->
                            purchaseResponse.titleSnapshot().equals("Finance Dataset")
                    );

            verify(userRepo).findById(buyerId);

            verify(purchaseRepo).findByBuyerIdAndGenre(
                    eq(buyerId),
                    eq(Genre.CODING),
                    argThat(pageable ->
                            pageable.getPageNumber() == 0
                                    && pageable.getPageSize() == 6
                    )
            );

            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(transactionService);
            verifyNoInteractions(storageDownloadService);
        }

        @Test
        void getPurchaseHistoryByUserId_nullGenre_treatsAsAllGenre() {
            Long buyerId = 1L;

            AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));

            Page<Purchase> emptyPage = new PageImpl<>(List.of());

            when(userRepo.findById(buyerId)).thenReturn(Optional.of(buyer));
            when(purchaseRepo.findByBuyerIdAndGenre(
                    eq(buyerId),
                    isNull(),
                    any(Pageable.class)
            )).thenReturn(emptyPage);

            PageResponse<PurchaseResponse> response = purchaseService.getPurchaseHistoryByUserId(
                    buyerId,
                    0,
                    6,
                    null,
                    "newest"
            );

            assertThat(response).isNotNull();
            assertThat(response.content()).isEmpty();

            verify(userRepo).findById(buyerId);

            verify(purchaseRepo).findByBuyerIdAndGenre(
                    eq(buyerId),
                    isNull(),
                    any(Pageable.class)
            );

            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(transactionService);
            verifyNoInteractions(storageDownloadService);
        }

        @Test
        void getPurchaseHistoryByUserId_userNotFound_throwsException() {

            Long userId = 1L;
            when(userRepo.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    purchaseService.getPurchaseHistoryByUserId(
                            userId,
                            0,
                            6,
                            "ALL",
                            "newest"
                    )
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User does not exist");

            verify(userRepo).findById(userId);

            verifyNoInteractions(purchaseRepo);
            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(transactionService);
            verifyNoInteractions(storageDownloadService);
        }

        @Test
        void getPurchaseHistoryByUserId_nonBusinessUser_throwsException() {
            Long userId = 1L;

            AppUser sellerUser = user(userId, "seller1", Role.SELLER, BigDecimal.ZERO);

            when(userRepo.findById(userId)).thenReturn(Optional.of(sellerUser));

            assertThatThrownBy(() ->
                    purchaseService.getPurchaseHistoryByUserId(
                            userId,
                            0,
                            6,
                            "ALL",
                            "newest"
                    )
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Only business users can view purchases");

            verify(userRepo).findById(userId);

            verifyNoInteractions(purchaseRepo);
            verifyNoInteractions(dataPostRepo);
            verifyNoInteractions(transactionService);
            verifyNoInteractions(storageDownloadService);
        }
    }

    @Nested
    class GeneratePurchaseDownloadUrlTests {

        @Test
        void generatePurchaseDownloadUrl_validPurchase_returnsDownloadResponse() {
            Long buyerId = 1L;
            Long sellerId = 2L;
            Long purchaseId = 100L;

            AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = user(sellerId, "seller1", Role.SELLER, BigDecimal.ZERO);
            DataPost post = post(10L, seller, "Dataset", "Summary", BigDecimal.valueOf(30), Genre.CODING);
            Purchase purchase = purchase(purchaseId, buyer, seller, post, BigDecimal.valueOf(30), "Dataset", "Summary");

            when(purchaseRepo.findByIdAndBuyerId(purchaseId, buyerId)).thenReturn(Optional.of(purchase));
            when(storageDownloadService.generateDownloadUrl(
                    "yelos-datasets",
                    "sellers/2/posts/test-file.csv",
                    "test.csv"
            )).thenReturn("https://signed-url.example.com");

            PurchaseDownloadResponse response = purchaseService.generatePurchaseDownloadUrl(
                    purchaseId,
                    buyerId
            );

            assertThat(response.downloadUrl()).isEqualTo("https://signed-url.example.com");
            assertThat(response.fileName()).isEqualTo("test.csv");
            assertThat(response.fileType()).isEqualTo("text/csv");
            assertThat(response.fileSize()).isEqualTo(36L);
            assertThat(response.expiresInSeconds()).isEqualTo(300);

            verify(purchaseRepo).findByIdAndBuyerId(purchaseId, buyerId);
            verify(storageDownloadService).generateDownloadUrl(
                    "yelos-datasets",
                    "sellers/2/posts/test-file.csv",
                    "test.csv"
            );
        }

        @Test
        void generatePurchaseDownloadUrl_purchaseNotFound_throwsException() {
            Long buyerId = 1L;
            Long purchaseId = 100L;

            AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));

            when(purchaseRepo.findByIdAndBuyerId(purchaseId, buyerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    purchaseService.generatePurchaseDownloadUrl(purchaseId, buyerId)
            ).isInstanceOf(RuntimeException.class);

            verify(purchaseRepo).findByIdAndBuyerId(purchaseId, buyerId);
            verifyNoInteractions(storageDownloadService);
        }

        @Test
        void generatePurchaseDownloadUrl_purchaseWithoutObjectKey_throwsException() {
            Long buyerId = 1L;
            Long sellerId = 2L;
            Long purchaseId = 100L;

            AppUser buyer = user(buyerId, "buyer1", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller = user(sellerId, "seller1", Role.SELLER, BigDecimal.ZERO);
            DataPost post = post(10L, seller, "Dataset", "Summary", BigDecimal.valueOf(30), Genre.CODING);
            Purchase purchase = purchase(purchaseId, buyer, seller, post, BigDecimal.valueOf(30), "Dataset", "Summary");

            ReflectionTestUtils.setField(purchase, "objectKeySnapshot", null);

            when(purchaseRepo.findByIdAndBuyerId(purchaseId, buyerId)).thenReturn(Optional.of(purchase));

            assertThatThrownBy(() ->
                    purchaseService.generatePurchaseDownloadUrl(purchaseId, buyerId)
            ).isInstanceOf(RuntimeException.class);

            verify(purchaseRepo).findByIdAndBuyerId(purchaseId, buyerId);
            verifyNoInteractions(storageDownloadService);
        }
    }

    // Helper functions
    private AppUser user(Long id, String username, Role role, BigDecimal credits) {
        AppUser user = new AppUser();

        //Use reflectiontestutils as id is auto incremented and set by JPA so to test id, we must set it with reflection test utils
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "username", username);
        ReflectionTestUtils.setField(user, "email", username + "@example.com");
        ReflectionTestUtils.setField(user, "passwordHash", "password123");
        ReflectionTestUtils.setField(user, "role", role);
        ReflectionTestUtils.setField(user, "credits", credits);

        return user;
    }

    private DataPost post(
            Long id,
            AppUser seller,
            String title,
            String summary,
            BigDecimal price,
            Genre genre
    ) {
        DataPost post = new DataPost();

        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "user", seller);
        ReflectionTestUtils.setField(post, "title", title);
        ReflectionTestUtils.setField(post, "summary", summary);
        ReflectionTestUtils.setField(post, "price", price);
        ReflectionTestUtils.setField(post, "genre", genre);

        ReflectionTestUtils.setField(post, "fileName", "test.csv");
        ReflectionTestUtils.setField(post, "fileType", "text/csv");
        ReflectionTestUtils.setField(post, "fileSize", 36L);
        ReflectionTestUtils.setField(post, "bucketName", "yelos-datasets");
        ReflectionTestUtils.setField(post, "objectKey", "sellers/2/posts/test-file.csv");

        return post;
    }

    private Purchase purchase(
            Long id,
            AppUser buyer,
            AppUser seller,
            DataPost post,
            BigDecimal pricePaid,
            String titleSnapshot,
            String summarySnapshot
    ) {
        Purchase purchase = new Purchase();
        ReflectionTestUtils.setField(purchase, "id", id);
        ReflectionTestUtils.setField(purchase, "buyer", buyer);
        ReflectionTestUtils.setField(purchase, "seller", seller);
        ReflectionTestUtils.setField(purchase, "post", post);
        ReflectionTestUtils.setField(purchase, "pricePaid", pricePaid);
        ReflectionTestUtils.setField(purchase, "titleSnapshot", titleSnapshot);
        ReflectionTestUtils.setField(purchase, "summarySnapshot", summarySnapshot);

        ReflectionTestUtils.setField(purchase, "fileNameSnapshot", "test.csv");
        ReflectionTestUtils.setField(purchase, "fileTypeSnapshot", "text/csv");
        ReflectionTestUtils.setField(purchase, "fileSizeSnapshot", 36L);
        ReflectionTestUtils.setField(purchase, "bucketNameSnapshot", "yelos-datasets");
        ReflectionTestUtils.setField(purchase, "objectKeySnapshot", "sellers/2/posts/test-file.csv");

        return purchase;
    }

    private Object getField(Object target, String fieldName) {
        return ReflectionTestUtils.getField(target, fieldName);
    }
}