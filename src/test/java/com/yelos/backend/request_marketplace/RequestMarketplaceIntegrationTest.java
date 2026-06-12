package com.yelos.backend.request_marketplace;

import com.yelos.backend.IntegrationTestBase;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.request_marketplace.models.DatasetRequest;
import com.yelos.backend.request_marketplace.models.RequestStatus;
import com.yelos.backend.request_marketplace.models.RequestSubmission;
import com.yelos.backend.request_marketplace.models.SubmissionStatus;
import com.yelos.backend.storage.dto.StoredFile;
import com.yelos.backend.storage.services.StorageService;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RequestMarketplaceIntegrationTest extends IntegrationTestBase {

    @MockitoBean
    private StorageService storageService;

    @Nested
    class FullRequestMarketplaceFlowTests {

        @Test
        void fullRequestMarketplaceFlow_shouldCreateSubmitAcceptAndTransferCredits() throws Exception {
            AppUser buyer = createUser(
                    "buyer",
                    "buyer@gmail.com",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            );

            AppUser seller = createUser(
                    "seller",
                    "seller@gmail.com",
                    Role.SELLER,
                    BigDecimal.valueOf(50)
            );

            // 1. Buyer creates dataset request
            mockMvc.perform(post("/api/requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "buyerId": %d,
                                      "genre": "CODING",
                                      "specification": "Need a Java coding interview dataset",
                                      "budget": 100
                                    }
                                    """.formatted(buyer.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.buyerId").value(buyer.getId()))
                    .andExpect(jsonPath("$.buyerUsername").value("buyer"))
                    .andExpect(jsonPath("$.genre").value("CODING"))
                    .andExpect(jsonPath("$.specification").value("Need a Java coding interview dataset"))
                    .andExpect(jsonPath("$.budget").value(100))
                    .andExpect(jsonPath("$.status").value("OPEN"));

            DatasetRequest savedRequest = datasetRequestRepo
                    .findByBuyerId(buyer.getId(), PageRequest.of(0, 10))
                    .getContent()
                    .get(0);

            assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.OPEN);
            assertThat(savedRequest.getBudget()).isEqualByComparingTo(BigDecimal.valueOf(100));

            // 2. Buyer can view own requests
            mockMvc.perform(get("/api/requests/buyer/{buyerId}", buyer.getId())
                            .param("page", "0")
                            .param("size", "6")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(savedRequest.getId()))
                    .andExpect(jsonPath("$.content[0].buyerId").value(buyer.getId()))
                    .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            // 3. Seller can view open requests
            mockMvc.perform(get("/api/requests/open")
                            .param("genre", "CODING")
                            .param("page", "0")
                            .param("size", "6")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(savedRequest.getId()))
                    .andExpect(jsonPath("$.content[0].genre").value("CODING"))
                    .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            // 4. Mock storage upload
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "sample.csv",
                    "text/csv",
                    "id,name\n1,Alice".getBytes()
            );

            StoredFile storedFile = new StoredFile(
                    "request-submissions",
                    "request-submissions/sample.csv",
                    "sample.csv",
                    "text/csv",
                    1024L,
                    Instant.now()
            );

            when(storageService.uploadDatasetFile(eq(seller.getId()), any(MultipartFile.class)))
                    .thenReturn(storedFile);

            // 5. Seller submits to request
            mockMvc.perform(multipart("/api/requests/{requestId}/submissions", savedRequest.getId())
                            .file(file)
                            .param("sellerId", String.valueOf(seller.getId()))
                            .param("message", "I can provide this dataset")
                            .param("proposedPrice", "80"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(savedRequest.getId()))
                    .andExpect(jsonPath("$.sellerId").value(seller.getId()))
                    .andExpect(jsonPath("$.sellerUsername").value("seller"))
                    .andExpect(jsonPath("$.message").value("I can provide this dataset"))
                    .andExpect(jsonPath("$.proposedPrice").value(80))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.fileName").value("sample.csv"))
                    .andExpect(jsonPath("$.fileType").value("text/csv"))
                    .andExpect(jsonPath("$.fileSize").value(1024));

            verify(storageService).uploadDatasetFile(eq(seller.getId()), any(MultipartFile.class));

            List<RequestSubmission> submissions =
                    requestSubmissionRepo.findByRequestId(savedRequest.getId());

            assertThat(submissions).hasSize(1);

            RequestSubmission savedSubmission = submissions.get(0);

            assertThat(savedSubmission.getStatus()).isEqualTo(SubmissionStatus.PENDING);
            assertThat(savedSubmission.getProposedPrice()).isEqualByComparingTo(BigDecimal.valueOf(80));
            assertThat(savedSubmission.getFileName()).isEqualTo("sample.csv");
            assertThat(savedSubmission.getBucketName()).isEqualTo("request-submissions");
            assertThat(savedSubmission.getObjectKey()).isEqualTo("request-submissions/sample.csv");

            // 6. Buyer views submissions for own request
            mockMvc.perform(get("/api/requests/{requestId}/submissions", savedRequest.getId())
                            .param("buyerId", String.valueOf(buyer.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(savedSubmission.getId()))
                    .andExpect(jsonPath("$[0].requestId").value(savedRequest.getId()))
                    .andExpect(jsonPath("$[0].sellerId").value(seller.getId()))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

            // 7. Seller views own submissions
            mockMvc.perform(get("/api/requests/seller/{sellerId}/submissions", seller.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(savedSubmission.getId()))
                    .andExpect(jsonPath("$[0].sellerId").value(seller.getId()))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

            // 8. Buyer accepts submission
            mockMvc.perform(post(
                            "/api/requests/{requestId}/submissions/{submissionId}/accept",
                            savedRequest.getId(),
                            savedSubmission.getId()
                    )
                            .param("buyerId", String.valueOf(buyer.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedSubmission.getId()))
                    .andExpect(jsonPath("$.requestId").value(savedRequest.getId()))
                    .andExpect(jsonPath("$.sellerId").value(seller.getId()))
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));

            // 9. Verify DB state
            DatasetRequest completedRequest = datasetRequestRepo
                    .findById(savedRequest.getId())
                    .orElseThrow();

            RequestSubmission acceptedSubmission = requestSubmissionRepo
                    .findById(savedSubmission.getId())
                    .orElseThrow();

            AppUser updatedBuyer = userRepo
                    .findById(buyer.getId())
                    .orElseThrow();

            AppUser updatedSeller = userRepo
                    .findById(seller.getId())
                    .orElseThrow();

            assertThat(completedRequest.getStatus()).isEqualTo(RequestStatus.COMPLETED);
            assertThat(completedRequest.getAcceptedSubmissionId()).isEqualTo(savedSubmission.getId());
            assertThat(completedRequest.getCompletedAt()).isNotNull();

            assertThat(acceptedSubmission.getStatus()).isEqualTo(SubmissionStatus.ACCEPTED);
            assertThat(acceptedSubmission.getAcceptedAt()).isNotNull();

            assertThat(updatedBuyer.getCredits()).isEqualByComparingTo(BigDecimal.valueOf(20));
            assertThat(updatedSeller.getCredits()).isEqualByComparingTo(BigDecimal.valueOf(130));
        }
    }

    @Nested
    class InvalidRequestMarketplaceFlowTests {

        @Test
        void submitToRequest_shouldReturnForbidden_whenUserIsNotSeller() throws Exception {
            AppUser buyer = createUser(
                    "buyer",
                    "buyer@gmail.com",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            );

            AppUser anotherBusinessUser = createUser(
                    "business2",
                    "business2@gmail.com",
                    Role.BUSINESS,
                    BigDecimal.valueOf(50)
            );

            DatasetRequest request = createDatasetRequest(
                    buyer,
                    Genre.CODING,
                    "Need coding dataset",
                    BigDecimal.valueOf(100)
            );

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "sample.csv",
                    "text/csv",
                    "id,name\n1,Alice".getBytes()
            );

            mockMvc.perform(multipart("/api/requests/{requestId}/submissions", request.getId())
                            .file(file)
                            .param("sellerId", String.valueOf(anotherBusinessUser.getId()))
                            .param("message", "I can provide this dataset")
                            .param("proposedPrice", "80"))
                    .andExpect(status().isForbidden());

            verify(storageService, never()).uploadDatasetFile(any(), any());

            List<RequestSubmission> submissions =
                    requestSubmissionRepo.findByRequestId(request.getId());

            assertThat(submissions).isEmpty();
        }

        @Test
        void acceptSubmission_shouldReturnBadRequest_whenBuyerHasInsufficientCredits() throws Exception {
            AppUser buyer = createUser(
                    "buyer",
                    "buyer@gmail.com",
                    Role.BUSINESS,
                    BigDecimal.valueOf(50)
            );

            AppUser seller = createUser(
                    "seller",
                    "seller@gmail.com",
                    Role.SELLER,
                    BigDecimal.valueOf(20)
            );

            DatasetRequest request = createDatasetRequest(
                    buyer,
                    Genre.CODING,
                    "Need coding dataset",
                    BigDecimal.valueOf(100)
            );

            RequestSubmission submission = createSubmission(
                    request,
                    seller,
                    BigDecimal.valueOf(80)
            );

            mockMvc.perform(post(
                            "/api/requests/{requestId}/submissions/{submissionId}/accept",
                            request.getId(),
                            submission.getId()
                    )
                            .param("buyerId", String.valueOf(buyer.getId())))
                    .andExpect(status().isBadRequest());

            DatasetRequest unchangedRequest = datasetRequestRepo
                    .findById(request.getId())
                    .orElseThrow();

            RequestSubmission unchangedSubmission = requestSubmissionRepo
                    .findById(submission.getId())
                    .orElseThrow();

            AppUser unchangedBuyer = userRepo
                    .findById(buyer.getId())
                    .orElseThrow();

            AppUser unchangedSeller = userRepo
                    .findById(seller.getId())
                    .orElseThrow();

            assertThat(unchangedRequest.getStatus()).isEqualTo(RequestStatus.OPEN);
            assertThat(unchangedRequest.getAcceptedSubmissionId()).isNull();

            assertThat(unchangedSubmission.getStatus()).isEqualTo(SubmissionStatus.PENDING);

            assertThat(unchangedBuyer.getCredits()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(unchangedSeller.getCredits()).isEqualByComparingTo(BigDecimal.valueOf(20));
        }
    }

    private AppUser createUser(
            String username,
            String email,
            Role role,
            BigDecimal credits
    ) {
        Instant now = Instant.now();

        AppUser user = new AppUser(
                username,
                email,
                "hashed-password",
                credits,
                role,
                now,
                now
        );

        return userRepo.save(user);
    }

    private DatasetRequest createDatasetRequest(
            AppUser buyer,
            Genre genre,
            String specification,
            BigDecimal budget
    ) {
        DatasetRequest request = new DatasetRequest(
                buyer,
                genre,
                specification,
                budget,
                Instant.now()
        );

        return datasetRequestRepo.save(request);
    }

    private RequestSubmission createSubmission(
            DatasetRequest request,
            AppUser seller,
            BigDecimal proposedPrice
    ) {
        RequestSubmission submission = new RequestSubmission(
                request,
                seller,
                "I can provide this dataset",
                proposedPrice,
                "sample.csv",
                "text/csv",
                1024L,
                "request-submissions",
                "request-submissions/sample.csv",
                Instant.now()
        );

        return requestSubmissionRepo.save(submission);
    }
}