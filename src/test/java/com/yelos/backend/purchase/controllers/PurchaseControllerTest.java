package com.yelos.backend.purchase.controllers;

import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.globaldto.PageResponse;
import com.yelos.backend.purchase.dto.PurchaseDownloadResponse;
import com.yelos.backend.purchase.dto.PurchasePostRequest;
import com.yelos.backend.purchase.dto.PurchaseResponse;
import com.yelos.backend.purchase.services.PurchaseService;
import com.yelos.backend.security.AuthUserPrincipal;
import com.yelos.backend.security.JwtService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseController.class)
@AutoConfigureMockMvc
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseService purchaseService;

    @MockitoBean
    private JwtService jwtService;

    @Nested
    class PostPurchaseTests {

        @Test
        void postPurchase_validRequest_returnsPurchaseResponse() throws Exception {
            Long buyerId = 1L;
            Long postId = 10L;

            PurchaseResponse response = purchaseResponse(
                    100L,
                    buyerId,
                    2L,
                    postId,
                    BigDecimal.valueOf(30),
                    BigDecimal.valueOf(70),
                    Genre.CODING,
                    "Dataset",
                    "Summary"
            );

            when(purchaseService.purchasePost(any(PurchasePostRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/purchase")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "buyerId": 1,
                                      "postId": 10
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100L))
                    .andExpect(jsonPath("$.buyerId").value(1L))
                    .andExpect(jsonPath("$.sellerId").value(2L))
                    .andExpect(jsonPath("$.postId").value(10L))
                    .andExpect(jsonPath("$.price").value(30))
                    .andExpect(jsonPath("$.buyerCredits").value(70))
                    .andExpect(jsonPath("$.genre").value("CODING"))
                    .andExpect(jsonPath("$.titleSnapshot").value("Dataset"))
                    .andExpect(jsonPath("$.summarySnapshot").value("Summary"))
                    .andExpect(jsonPath("$.fileNameSnapshot").value("test.csv"))
                    .andExpect(jsonPath("$.fileTypeSnapshot").value("text/csv"))
                    .andExpect(jsonPath("$.fileSizeSnapshot").value(36))
                    .andExpect(jsonPath("$.purchasedAt").exists());

            verify(purchaseService).purchasePost(argThat(req ->
                    req.buyerId().equals(buyerId)
                            && req.postId().equals(postId)
            ));
        }
    }

    @Nested
    class GetPurchaseHistoryByUserIdTests {

        @Test
        void getPurchaseHistoryByUserId_withAllParams_returnsPageResponse() throws Exception {
            Long userId = 1L;

            PurchaseResponse purchase = purchaseResponse(
                    100L,
                    userId,
                    2L,
                    10L,
                    BigDecimal.valueOf(30),
                    BigDecimal.valueOf(70),
                    Genre.CODING,
                    "Dataset",
                    "Summary"
            );

            PageResponse<PurchaseResponse> pageResponse = new PageResponse<>(
                    List.of(purchase),
                    0,
                    6,
                    1,
                    1,
                    true
            );

            when(purchaseService.getPurchaseHistoryByUserId(
                    eq(userId),
                    eq(0),
                    eq(6),
                    eq("CODING"),
                    eq("newest")
            )).thenReturn(pageResponse);

            mockMvc.perform(get("/api/purchase/users/{userId}/history", userId)
                            .param("page", "0")
                            .param("size", "6")
                            .param("genre", "CODING")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(100L))
                    .andExpect(jsonPath("$.content[0].buyerId").value(1L))
                    .andExpect(jsonPath("$.content[0].sellerId").value(2L))
                    .andExpect(jsonPath("$.content[0].postId").value(10L))
                    .andExpect(jsonPath("$.content[0].price").value(30))
                    .andExpect(jsonPath("$.content[0].buyerCredits").value(70))
                    .andExpect(jsonPath("$.content[0].genre").value("CODING"))
                    .andExpect(jsonPath("$.content[0].titleSnapshot").value("Dataset"))
                    .andExpect(jsonPath("$.content[0].summarySnapshot").value("Summary"))
                    .andExpect(jsonPath("$.content[0].fileNameSnapshot").value("test.csv"))
                    .andExpect(jsonPath("$.content[0].fileTypeSnapshot").value("text/csv"))
                    .andExpect(jsonPath("$.content[0].fileSizeSnapshot").value(36))
                    .andExpect(jsonPath("$.content[0].purchasedAt").exists())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(6))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.last").value(true));

            verify(purchaseService).getPurchaseHistoryByUserId(
                    eq(userId),
                    eq(0),
                    eq(6),
                    eq("CODING"),
                    eq("newest")
            );
        }

        @Test
        void getPurchaseHistoryByUserId_withoutParams_usesDefaultValues() throws Exception {
            Long userId = 1L;

            PageResponse<PurchaseResponse> pageResponse = new PageResponse<>(
                    List.of(),
                    0,
                    12,
                    0,
                    0,
                    true
            );

            when(purchaseService.getPurchaseHistoryByUserId(
                    eq(userId),
                    eq(0),
                    eq(12),
                    eq("ALL"),
                    eq("newest")
            )).thenReturn(pageResponse);

            mockMvc.perform(get("/api/purchase/users/{userId}/history", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(12))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.last").value(true));

            verify(purchaseService).getPurchaseHistoryByUserId(
                    eq(userId),
                    eq(0),
                    eq(12),
                    eq("ALL"),
                    eq("newest")
            );
        }
    }

    @Nested
    class GetPurchasesByUserIdTests {

        @Test
        void getPurchasesByUserId_validUserId_returnsPurchaseList() throws Exception {
            Long userId = 1L;

            PurchaseResponse purchase1 = purchaseResponse(
                    100L,
                    userId,
                    2L,
                    10L,
                    BigDecimal.valueOf(30),
                    BigDecimal.valueOf(70),
                    Genre.CODING,
                    "Dataset 1",
                    "Summary 1"
            );

            PurchaseResponse purchase2 = purchaseResponse(
                    101L,
                    userId,
                    2L,
                    11L,
                    BigDecimal.valueOf(40),
                    BigDecimal.valueOf(30),
                    Genre.FINANCE,
                    "Dataset 2",
                    "Summary 2"
            );

            when(purchaseService.getPurchasesByUserId(userId))
                    .thenReturn(List.of(purchase1, purchase2));

            mockMvc.perform(get("/api/purchase/users/{userId}/posts", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))

                    .andExpect(jsonPath("$[0].id").value(100L))
                    .andExpect(jsonPath("$[0].buyerId").value(1L))
                    .andExpect(jsonPath("$[0].sellerId").value(2L))
                    .andExpect(jsonPath("$[0].postId").value(10L))
                    .andExpect(jsonPath("$[0].price").value(30))
                    .andExpect(jsonPath("$[0].buyerCredits").value(70))
                    .andExpect(jsonPath("$[0].genre").value("CODING"))
                    .andExpect(jsonPath("$[0].titleSnapshot").value("Dataset 1"))
                    .andExpect(jsonPath("$[0].summarySnapshot").value("Summary 1"))
                    .andExpect(jsonPath("$[0].fileNameSnapshot").value("test.csv"))
                    .andExpect(jsonPath("$[0].fileTypeSnapshot").value("text/csv"))
                    .andExpect(jsonPath("$[0].fileSizeSnapshot").value(36))
                    .andExpect(jsonPath("$[0].purchasedAt").exists())

                    .andExpect(jsonPath("$[1].id").value(101L))
                    .andExpect(jsonPath("$[1].buyerId").value(1L))
                    .andExpect(jsonPath("$[1].sellerId").value(2L))
                    .andExpect(jsonPath("$[1].postId").value(11L))
                    .andExpect(jsonPath("$[1].price").value(40))
                    .andExpect(jsonPath("$[1].buyerCredits").value(30))
                    .andExpect(jsonPath("$[1].genre").value("FINANCE"))
                    .andExpect(jsonPath("$[1].titleSnapshot").value("Dataset 2"))
                    .andExpect(jsonPath("$[1].summarySnapshot").value("Summary 2"))
                    .andExpect(jsonPath("$[1].fileNameSnapshot").value("test.csv"))
                    .andExpect(jsonPath("$[1].fileTypeSnapshot").value("text/csv"))
                    .andExpect(jsonPath("$[1].fileSizeSnapshot").value(36))
                    .andExpect(jsonPath("$[1].purchasedAt").exists());

            verify(purchaseService).getPurchasesByUserId(userId);
        }
    }

    @Nested
    class DownloadPurchasedFileTests {
        @Test
        void downloadPurchasedFile_validPrincipal_returnsDownloadResponse() {
            Long purchaseId = 100L;
            Long buyerId = 1L;

            AuthUserPrincipal principal = new AuthUserPrincipal(
                    buyerId,
                    "buyer1",
                    "BUSINESS"
            );

            PurchaseDownloadResponse response = new PurchaseDownloadResponse(
                    "https://signed-url.example.com",
                    "test.csv",
                    "text/csv",
                    36L,
                    300
            );

            when(purchaseService.generatePurchaseDownloadUrl(purchaseId, buyerId))
                    .thenReturn(response);

            PurchaseController controller = new PurchaseController(purchaseService);

            PurchaseDownloadResponse result = controller.downloadPurchasedFile(
                    purchaseId,
                    principal
            );

            assertEquals("https://signed-url.example.com", result.downloadUrl());
            assertEquals("test.csv", result.fileName());
            assertEquals("text/csv", result.fileType());
            assertEquals(36L, result.fileSize());
            assertEquals(300, result.expiresInSeconds());

            verify(purchaseService).generatePurchaseDownloadUrl(purchaseId, buyerId);
        }
    }

    private PurchaseResponse purchaseResponse(
            Long id,
            Long buyerId,
            Long sellerId,
            Long postId,
            BigDecimal price,
            BigDecimal buyerCredits,
            Genre genre,
            String titleSnapshot,
            String summarySnapshot
    ) {
        return new PurchaseResponse(
                id,
                buyerId,
                sellerId,
                postId,
                price,
                buyerCredits,
                genre,
                titleSnapshot,
                summarySnapshot,
                "test.csv",
                "text/csv",
                36L,
                Instant.now()
        );
    }
}