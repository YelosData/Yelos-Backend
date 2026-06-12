package com.yelos.backend.request_marketplace.controllers;

import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.request_marketplace.dto.DatasetRequestRequest;
import com.yelos.backend.request_marketplace.dto.DatasetRequestResponse;
import com.yelos.backend.request_marketplace.dto.RequestSubmissionDownloadResponse;
import com.yelos.backend.request_marketplace.dto.RequestSubmissionResponse;
import com.yelos.backend.request_marketplace.models.RequestStatus;
import com.yelos.backend.request_marketplace.models.SubmissionStatus;
import com.yelos.backend.request_marketplace.services.DatasetRequestService;
import com.yelos.backend.request_marketplace.services.RequestSubmissionService;
import com.yelos.backend.security.AuthUserPrincipal;
import com.yelos.backend.security.JwtService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DatasetRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class DatasetRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatasetRequestService datasetRequestService;

    @MockitoBean
    private RequestSubmissionService requestSubmissionService;

    @MockitoBean
    private JwtService jwtService;

    @Nested
    class CreateRequestTests {

        @Test
        void createRequest_shouldReturnCreatedDatasetRequest() throws Exception {
            DatasetRequestResponse response = createDatasetRequestResponse(
                    10L,
                    1L,
                    "buyer",
                    Genre.CODING,
                    "Need coding dataset",
                    BigDecimal.valueOf(100),
                    RequestStatus.OPEN
            );

            when(datasetRequestService.createRequest(any(DatasetRequestRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                            {
                              "buyerId": 1,
                              "genre": "CODING",
                              "specification": "Need coding dataset",
                              "budget": 100
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.buyerId").value(1))
                    .andExpect(jsonPath("$.buyerUsername").value("buyer"))
                    .andExpect(jsonPath("$.genre").value("CODING"))
                    .andExpect(jsonPath("$.specification").value("Need coding dataset"))
                    .andExpect(jsonPath("$.budget").value(100))
                    .andExpect(jsonPath("$.status").value("OPEN"));

            ArgumentCaptor<DatasetRequestRequest> captor =
                    ArgumentCaptor.forClass(DatasetRequestRequest.class);

            verify(datasetRequestService).createRequest(captor.capture());

            assertThat(captor.getValue().buyerId()).isEqualTo(1L);
            assertThat(captor.getValue().genre()).isEqualTo(Genre.CODING);
            assertThat(captor.getValue().specification()).isEqualTo("Need coding dataset");
            assertThat(captor.getValue().budget()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }
    }

    @Nested
    class GetBuyerRequestsTests {

        @Test
        void getBuyerRequests_shouldReturnPagedBuyerRequests() throws Exception {
            DatasetRequestResponse response = createDatasetRequestResponse(
                    10L,
                    1L,
                    "buyer",
                    Genre.CODING,
                    "Need coding dataset",
                    BigDecimal.valueOf(100),
                    RequestStatus.OPEN
            );

            Page<DatasetRequestResponse> page = new PageImpl<>(
                    List.of(response),
                    org.springframework.data.domain.PageRequest.of(0, 6),
                    1
            );

            when(datasetRequestService.getBuyerRequests(eq(1L), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/requests/buyer/{buyerId}", 1L)
                            .param("page", "0")
                            .param("size", "6")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(10))
                    .andExpect(jsonPath("$.content[0].buyerId").value(1))
                    .andExpect(jsonPath("$.content[0].genre").value("CODING"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(6))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.last").value(true));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            verify(datasetRequestService).getBuyerRequests(eq(1L), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();

            assertThat(pageable.getPageNumber()).isEqualTo(0);
            assertThat(pageable.getPageSize()).isEqualTo(6);
            assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
        }

        @Test
        void getBuyerRequests_shouldUseBudgetAscendingSort_whenSortIsBudgetAsc() throws Exception {
            Page<DatasetRequestResponse> page = new PageImpl<>(List.of());

            when(datasetRequestService.getBuyerRequests(eq(1L), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/requests/buyer/{buyerId}", 1L)
                            .param("page", "0")
                            .param("size", "6")
                            .param("sort", "budgetAsc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            verify(datasetRequestService).getBuyerRequests(eq(1L), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();

            assertThat(pageable.getSort().getOrderFor("budget")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("budget").isAscending()).isTrue();
        }

        @Test
        void getBuyerRequests_shouldUseBudgetDescendingSort_whenSortIsBudgetDesc() throws Exception {
            Page<DatasetRequestResponse> page = new PageImpl<>(List.of());

            when(datasetRequestService.getBuyerRequests(eq(1L), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/requests/buyer/{buyerId}", 1L)
                            .param("page", "0")
                            .param("size", "6")
                            .param("sort", "budgetDesc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            verify(datasetRequestService).getBuyerRequests(eq(1L), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();

            assertThat(pageable.getSort().getOrderFor("budget")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("budget").isDescending()).isTrue();
        }
    }

    @Nested
    class GetOpenRequestsTests {

        @Test
        void getOpenRequests_shouldPassNullGenre_whenGenreIsAll() throws Exception {
            DatasetRequestResponse response = createDatasetRequestResponse(
                    10L,
                    1L,
                    "buyer",
                    Genre.CODING,
                    "Need coding dataset",
                    BigDecimal.valueOf(100),
                    RequestStatus.OPEN
            );

            Page<DatasetRequestResponse> page = new PageImpl<>(
                    List.of(response),
                    org.springframework.data.domain.PageRequest.of(0, 12),
                    1
            );

            when(datasetRequestService.getOpenRequests(isNull(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/requests/open")
                            .param("genre", "ALL")
                            .param("page", "0")
                            .param("size", "12")
                            .param("sort", "newest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(10))
                    .andExpect(jsonPath("$.content[0].status").value("OPEN"));

            verify(datasetRequestService).getOpenRequests(isNull(), any(Pageable.class));
        }

        @Test
        void getOpenRequests_shouldParseGenre_whenGenreIsProvided() throws Exception {
            Page<DatasetRequestResponse> page = new PageImpl<>(List.of());

            when(datasetRequestService.getOpenRequests(eq(Genre.CODING), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/requests/open")
                            .param("genre", "CODING")
                            .param("page", "0")
                            .param("size", "12")
                            .param("sort", "newest"))
                    .andExpect(status().isOk());

            verify(datasetRequestService).getOpenRequests(eq(Genre.CODING), any(Pageable.class));
        }
    }

    @Nested
    class SubmitToRequestTests {

        @Test
        void submitToRequest_shouldSubmitMultipartRequest() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "sample.csv",
                    "text/csv",
                    "id,name\n1,Alice".getBytes()
            );

            RequestSubmissionResponse response = createSubmissionResponse(
                    99L,
                    10L,
                    2L,
                    "seller",
                    "I can provide this dataset",
                    BigDecimal.valueOf(80),
                    SubmissionStatus.PENDING
            );

            when(requestSubmissionService.submitToRequest(
                    eq(10L),
                    eq(2L),
                    eq("I can provide this dataset"),
                    eq(BigDecimal.valueOf(80)),
                    any(MultipartFile.class)
            )).thenReturn(response);

            mockMvc.perform(multipart("/api/requests/{requestId}/submissions", 10L)
                            .file(file)
                            .param("sellerId", "2")
                            .param("message", "I can provide this dataset")
                            .param("proposedPrice", "80"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(99))
                    .andExpect(jsonPath("$.requestId").value(10))
                    .andExpect(jsonPath("$.sellerId").value(2))
                    .andExpect(jsonPath("$.sellerUsername").value("seller"))
                    .andExpect(jsonPath("$.message").value("I can provide this dataset"))
                    .andExpect(jsonPath("$.proposedPrice").value(80))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.fileName").value("sample.csv"))
                    .andExpect(jsonPath("$.fileType").value("text/csv"))
                    .andExpect(jsonPath("$.fileSize").value(1024));

            verify(requestSubmissionService).submitToRequest(
                    eq(10L),
                    eq(2L),
                    eq("I can provide this dataset"),
                    eq(BigDecimal.valueOf(80)),
                    any(MultipartFile.class)
            );
        }
    }

    @Nested
    class GetRequestSubmissionsTests {

        @Test
        void getRequestSubmissions_shouldReturnSubmissionsForRequest() throws Exception {
            RequestSubmissionResponse response = createSubmissionResponse(
                    99L,
                    10L,
                    2L,
                    "seller",
                    "I can provide this dataset",
                    BigDecimal.valueOf(80),
                    SubmissionStatus.PENDING
            );

            when(requestSubmissionService.getRequestSubmissions(10L, 1L))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/requests/{requestId}/submissions", 10L)
                            .param("buyerId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(99))
                    .andExpect(jsonPath("$[0].requestId").value(10))
                    .andExpect(jsonPath("$[0].sellerId").value(2));

            verify(requestSubmissionService).getRequestSubmissions(10L, 1L);
        }
    }

    @Nested
    class GetSellerSubmissionsTests {

        @Test
        void getSellerSubmissions_shouldReturnSellerSubmissions() throws Exception {
            RequestSubmissionResponse response = createSubmissionResponse(
                    99L,
                    10L,
                    2L,
                    "seller",
                    "I can provide this dataset",
                    BigDecimal.valueOf(80),
                    SubmissionStatus.PENDING
            );

            when(requestSubmissionService.getSellerSubmissions(2L))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/requests/seller/{sellerId}/submissions", 2L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(99))
                    .andExpect(jsonPath("$[0].sellerId").value(2));

            verify(requestSubmissionService).getSellerSubmissions(2L);
        }
    }

    @Nested
    class AcceptSubmissionTests {

        @Test
        void acceptSubmission_shouldAcceptSubmission() throws Exception {
            RequestSubmissionResponse response = createSubmissionResponse(
                    99L,
                    10L,
                    2L,
                    "seller",
                    "I can provide this dataset",
                    BigDecimal.valueOf(80),
                    SubmissionStatus.ACCEPTED
            );

            when(requestSubmissionService.acceptSubmission(10L, 99L, 1L))
                    .thenReturn(response);

            mockMvc.perform(post("/api/requests/{requestId}/submissions/{submissionId}/accept", 10L, 99L)
                            .param("buyerId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(99))
                    .andExpect(jsonPath("$.requestId").value(10))
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));

            verify(requestSubmissionService).acceptSubmission(10L, 99L, 1L);
        }
    }

    private DatasetRequestResponse createDatasetRequestResponse(
            Long id,
            Long buyerId,
            String buyerUsername,
            Genre genre,
            String specification,
            BigDecimal budget,
            RequestStatus status
    ) {
        Instant now = Instant.now();

        return new DatasetRequestResponse(
                id,
                buyerId,
                buyerUsername,
                genre,
                specification,
                budget,
                status,
                null,
                now,
                now,
                null
        );
    }

    private RequestSubmissionResponse createSubmissionResponse(
            Long id,
            Long requestId,
            Long sellerId,
            String sellerUsername,
            String message,
            BigDecimal proposedPrice,
            SubmissionStatus status
    ) {
        Instant now = Instant.now();

        return new RequestSubmissionResponse(
                id,
                requestId,
                sellerId,
                sellerUsername,
                message,
                proposedPrice,
                status,
                "sample.csv",
                "text/csv",
                1024L,
                now,
                status == SubmissionStatus.ACCEPTED ? now : null,
                status == SubmissionStatus.REJECTED ? now : null
        );
    }

    private Authentication createAuthentication(Long userId) {
        AuthUserPrincipal principal = new AuthUserPrincipal(
                userId,
                "buyer",
                "BUSINESS"
        );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_BUSINESS"))
        );
    }
}