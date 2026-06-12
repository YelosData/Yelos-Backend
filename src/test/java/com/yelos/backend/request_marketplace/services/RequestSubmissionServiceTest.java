package com.yelos.backend.request_marketplace.services;

import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.request_marketplace.dto.RequestSubmissionDownloadResponse;
import com.yelos.backend.request_marketplace.dto.RequestSubmissionResponse;
import com.yelos.backend.request_marketplace.models.DatasetRequest;
import com.yelos.backend.request_marketplace.models.RequestSubmission;
import com.yelos.backend.request_marketplace.models.SubmissionStatus;
import com.yelos.backend.request_marketplace.repo.DatasetRequestRepo;
import com.yelos.backend.request_marketplace.repo.RequestSubmissionRepo;
import com.yelos.backend.storage.dto.StoredFile;
import com.yelos.backend.storage.services.StorageDownloadService;
import com.yelos.backend.storage.services.StorageService;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import com.yelos.backend.user.repo.UserRepo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestSubmissionServiceTest {

    @Mock
    private RequestSubmissionRepo requestSubmissionRepo;

    @Mock
    private DatasetRequestRepo datasetRequestRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private StorageService storageService;

    @Mock
    private StorageDownloadService storageDownloadService;

    @InjectMocks
    private RequestSubmissionService requestSubmissionService;

    @Nested
    class SubmitToRequestTests {

        @Test
        void submitToRequest_shouldCreateSubmission_whenRequestIsOpenAndSellerIsValid() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(1000));
            AppUser seller = createUser(2L, "seller", Role.SELLER, BigDecimal.valueOf(500));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            MockMultipartFile file = createFile();

            StoredFile storedFile = new StoredFile(
                    "request-submissions",
                    "request-submissions/sample.csv",
                    "sample.csv",
                    "text/csv",
                    1024L,
                    Instant.now()
            );

            when(datasetRequestRepo.findById(10L)).thenReturn(Optional.of(request));
            when(userRepo.findById(2L)).thenReturn(Optional.of(seller));
            when(requestSubmissionRepo.existsByRequestIdAndSellerId(10L, 2L)).thenReturn(false);
            when(storageService.uploadDatasetFile(2L, file)).thenReturn(storedFile);

            when(requestSubmissionRepo.save(any(RequestSubmission.class)))
                    .thenAnswer(invocation -> {
                        RequestSubmission savedSubmission = invocation.getArgument(0);
                        ReflectionTestUtils.setField(savedSubmission, "id", 99L);
                        return savedSubmission;
                    });

            RequestSubmissionResponse response = requestSubmissionService.submitToRequest(
                    10L,
                    2L,
                    "   I can provide this dataset   ",
                    BigDecimal.valueOf(80),
                    file
            );

            assertThat(response.id()).isEqualTo(99L);
            assertThat(response.requestId()).isEqualTo(10L);
            assertThat(response.sellerId()).isEqualTo(2L);
            assertThat(response.sellerUsername()).isEqualTo("seller");
            assertThat(response.message()).isEqualTo("I can provide this dataset");
            assertThat(response.proposedPrice()).isEqualByComparingTo(BigDecimal.valueOf(80));
            assertThat(response.status()).isEqualTo(SubmissionStatus.PENDING);
            assertThat(response.fileName()).isEqualTo("sample.csv");
            assertThat(response.fileType()).isEqualTo("text/csv");
            assertThat(response.fileSize()).isEqualTo(1024L);

            verify(storageService).uploadDatasetFile(2L, file);
            verify(requestSubmissionRepo).save(any(RequestSubmission.class));
        }

        @Test
        void submitToRequest_shouldThrowNotFound_whenRequestDoesNotExist() {
            MockMultipartFile file = createFile();

            when(datasetRequestRepo.findById(10L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> requestSubmissionService.submitToRequest(
                            10L,
                            2L,
                            "message",
                            BigDecimal.valueOf(80),
                            file
                    )
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getReason()).isEqualTo("Dataset request does not exist");

            verify(userRepo, never()).findById(anyLong());
            verify(storageService, never()).uploadDatasetFile(anyLong(), any());
            verify(requestSubmissionRepo, never()).save(any());
        }

        @Test
        void submitToRequest_shouldThrowForbidden_whenUserIsNotSeller() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(1000));
            AppUser businessUser = createUser(2L, "business2", Role.BUSINESS, BigDecimal.valueOf(500));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            MockMultipartFile file = createFile();

            when(datasetRequestRepo.findById(10L)).thenReturn(Optional.of(request));
            when(userRepo.findById(2L)).thenReturn(Optional.of(businessUser));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> requestSubmissionService.submitToRequest(
                            10L,
                            2L,
                            "message",
                            BigDecimal.valueOf(80),
                            file
                    )
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getReason()).isEqualTo("Only sellers can submit to dataset requests");

            verify(storageService, never()).uploadDatasetFile(anyLong(), any());
            verify(requestSubmissionRepo, never()).save(any());
        }

        @Test
        void submitToRequest_shouldThrowBadRequest_whenSellerAlreadySubmitted() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(1000));
            AppUser seller = createUser(2L, "seller", Role.SELLER, BigDecimal.valueOf(500));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            MockMultipartFile file = createFile();

            when(datasetRequestRepo.findById(10L)).thenReturn(Optional.of(request));
            when(userRepo.findById(2L)).thenReturn(Optional.of(seller));
            when(requestSubmissionRepo.existsByRequestIdAndSellerId(10L, 2L)).thenReturn(true);

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> requestSubmissionService.submitToRequest(
                            10L,
                            2L,
                            "message",
                            BigDecimal.valueOf(80),
                            file
                    )
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).isEqualTo("You have already submitted to this request");

            verify(storageService, never()).uploadDatasetFile(anyLong(), any());
            verify(requestSubmissionRepo, never()).save(any());
        }

        @Test
        void submitToRequest_shouldThrowBadRequest_whenProposedPriceExceedsBudget() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(1000));
            AppUser seller = createUser(2L, "seller", Role.SELLER, BigDecimal.valueOf(500));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            MockMultipartFile file = createFile();

            when(datasetRequestRepo.findById(10L)).thenReturn(Optional.of(request));
            when(userRepo.findById(2L)).thenReturn(Optional.of(seller));
            when(requestSubmissionRepo.existsByRequestIdAndSellerId(10L, 2L)).thenReturn(false);

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> requestSubmissionService.submitToRequest(
                            10L,
                            2L,
                            "message",
                            BigDecimal.valueOf(150),
                            file
                    )
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).isEqualTo("Proposed price cannot exceed request budget");

            verify(storageService, never()).uploadDatasetFile(anyLong(), any());
            verify(requestSubmissionRepo, never()).save(any());
        }

        @Test
        void submitToRequest_shouldThrowBadRequest_whenFileIsEmpty() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(1000));
            AppUser seller = createUser(2L, "seller", Role.SELLER, BigDecimal.valueOf(500));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.csv",
                    "text/csv",
                    new byte[]{}
            );

            when(datasetRequestRepo.findById(10L)).thenReturn(Optional.of(request));
            when(userRepo.findById(2L)).thenReturn(Optional.of(seller));
            when(requestSubmissionRepo.existsByRequestIdAndSellerId(10L, 2L)).thenReturn(false);

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> requestSubmissionService.submitToRequest(
                            10L,
                            2L,
                            "message",
                            BigDecimal.valueOf(80),
                            emptyFile
                    )
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).isEqualTo("Dataset file is required");

            verify(storageService, never()).uploadDatasetFile(anyLong(), any());
            verify(requestSubmissionRepo, never()).save(any());
        }
    }

    @Nested
    class GetRequestSubmissionsTests {

        @Test
        void getRequestSubmissions_shouldReturnSubmissions_whenBuyerOwnsRequest() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(1000));
            AppUser seller = createUser(2L, "seller", Role.SELLER, BigDecimal.valueOf(500));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            RequestSubmission submission = createSubmission(
                    99L,
                    request,
                    seller,
                    BigDecimal.valueOf(80),
                    SubmissionStatus.PENDING
            );

            when(datasetRequestRepo.findById(10L)).thenReturn(Optional.of(request));
            when(requestSubmissionRepo.findByRequestId(10L)).thenReturn(List.of(submission));

            List<RequestSubmissionResponse> result = requestSubmissionService.getRequestSubmissions(
                    10L,
                    1L
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(99L);
            assertThat(result.get(0).sellerId()).isEqualTo(2L);
        }

        @Test
        void getRequestSubmissions_shouldThrowForbidden_whenBuyerDoesNotOwnRequest() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(1000));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            when(datasetRequestRepo.findById(10L)).thenReturn(Optional.of(request));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> requestSubmissionService.getRequestSubmissions(10L, 999L)
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getReason()).isEqualTo("You can only view submissions for your own request");

            verify(requestSubmissionRepo, never()).findByRequestId(anyLong());
        }
    }

    @Nested
    class GetSellerSubmissionsTests {

        @Test
        void getSellerSubmissions_shouldReturnSellerSubmissions_whenUserIsSeller() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(1000));
            AppUser seller = createUser(2L, "seller", Role.SELLER, BigDecimal.valueOf(500));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            RequestSubmission submission = createSubmission(
                    99L,
                    request,
                    seller,
                    BigDecimal.valueOf(80),
                    SubmissionStatus.PENDING
            );

            when(userRepo.findById(2L)).thenReturn(Optional.of(seller));
            when(requestSubmissionRepo.findBySellerId(2L)).thenReturn(List.of(submission));

            List<RequestSubmissionResponse> result = requestSubmissionService.getSellerSubmissions(2L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(99L);
            assertThat(result.get(0).requestId()).isEqualTo(10L);
        }

        @Test
        void getSellerSubmissions_shouldThrowForbidden_whenUserIsNotSeller() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(1000));

            when(userRepo.findById(1L)).thenReturn(Optional.of(buyer));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> requestSubmissionService.getSellerSubmissions(1L)
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getReason()).isEqualTo("Only sellers can view their request submissions");

            verify(requestSubmissionRepo, never()).findBySellerId(anyLong());
        }
    }

    @Nested
    class AcceptSubmissionTests {

        @Test
        void acceptSubmission_shouldAcceptSubmissionRejectOtherPendingSubmissionsAndTransferCredits() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(100));
            AppUser seller1 = createUser(2L, "seller1", Role.SELLER, BigDecimal.valueOf(50));
            AppUser seller2 = createUser(3L, "seller2", Role.SELLER, BigDecimal.valueOf(20));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            RequestSubmission acceptedSubmission = createSubmission(
                    99L,
                    request,
                    seller1,
                    BigDecimal.valueOf(80),
                    SubmissionStatus.PENDING
            );

            RequestSubmission otherPendingSubmission = createSubmission(
                    100L,
                    request,
                    seller2,
                    BigDecimal.valueOf(70),
                    SubmissionStatus.PENDING
            );

            when(datasetRequestRepo.findById(10L)).thenReturn(Optional.of(request));
            when(userRepo.findById(1L)).thenReturn(Optional.of(buyer));
            when(requestSubmissionRepo.findByIdAndRequestId(99L, 10L))
                    .thenReturn(Optional.of(acceptedSubmission));
            when(requestSubmissionRepo.findByRequestIdAndStatus(10L, SubmissionStatus.PENDING))
                    .thenReturn(List.of(acceptedSubmission, otherPendingSubmission));

            when(requestSubmissionRepo.save(any(RequestSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(requestSubmissionRepo.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(datasetRequestRepo.save(any(DatasetRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RequestSubmissionResponse response = requestSubmissionService.acceptSubmission(
                    10L,
                    99L,
                    1L
            );

            assertThat(response.id()).isEqualTo(99L);
            assertThat(response.status()).isEqualTo(SubmissionStatus.ACCEPTED);

            assertThat(acceptedSubmission.getStatus()).isEqualTo(SubmissionStatus.ACCEPTED);
            assertThat(otherPendingSubmission.getStatus()).isEqualTo(SubmissionStatus.REJECTED);

            assertThat(request.getStatus().name()).isEqualTo("COMPLETED");
            assertThat(request.getAcceptedSubmissionId()).isEqualTo(99L);

            assertThat(buyer.getCredits()).isEqualByComparingTo(BigDecimal.valueOf(20));
            assertThat(seller1.getCredits()).isEqualByComparingTo(BigDecimal.valueOf(130));

            verify(userRepo).save(buyer);
            verify(userRepo).save(seller1);
            verify(requestSubmissionRepo).save(acceptedSubmission);
            verify(requestSubmissionRepo).saveAll(List.of(acceptedSubmission, otherPendingSubmission));
            verify(datasetRequestRepo).save(request);
        }

        @Test
        void acceptSubmission_shouldThrowBadRequest_whenBuyerHasInsufficientCredits() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(50));
            AppUser seller = createUser(2L, "seller", Role.SELLER, BigDecimal.valueOf(50));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            RequestSubmission submission = createSubmission(
                    99L,
                    request,
                    seller,
                    BigDecimal.valueOf(80),
                    SubmissionStatus.PENDING
            );

            when(datasetRequestRepo.findById(10L)).thenReturn(Optional.of(request));
            when(userRepo.findById(1L)).thenReturn(Optional.of(buyer));
            when(requestSubmissionRepo.findByIdAndRequestId(99L, 10L))
                    .thenReturn(Optional.of(submission));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> requestSubmissionService.acceptSubmission(
                            10L,
                            99L,
                            1L
                    )
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).isEqualTo("Buyer does not have enough credits");

            verify(userRepo, never()).save(any());
            verify(datasetRequestRepo, never()).save(any());
            verify(requestSubmissionRepo, never()).save(any());
        }
    }

    @Nested
    class GenerateSubmissionDownloadUrlTests {

        @Test
        void generateSubmissionDownloadUrl_shouldReturnDownloadUrl_whenSubmissionIsAcceptedForCompletedRequest() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(1000));
            AppUser seller = createUser(2L, "seller", Role.SELLER, BigDecimal.valueOf(500));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            RequestSubmission submission = createSubmission(
                    99L,
                    request,
                    seller,
                    BigDecimal.valueOf(80),
                    SubmissionStatus.PENDING
            );

            submission.accept(Instant.now());
            request.complete(99L, Instant.now());

            when(requestSubmissionRepo.findById(99L)).thenReturn(Optional.of(submission));
            when(storageDownloadService.generateDownloadUrl(
                    "request-submissions",
                    "request-submissions/sample.csv",
                    "sample.csv"
            )).thenReturn("https://signed-url.com/sample.csv");

            RequestSubmissionDownloadResponse response =
                    requestSubmissionService.generateSubmissionDownloadUrl(
                            99L,
                            1L
                    );

            assertThat(response.downloadUrl()).isEqualTo("https://signed-url.com/sample.csv");
            assertThat(response.fileName()).isEqualTo("sample.csv");
            assertThat(response.fileType()).isEqualTo("text/csv");
            assertThat(response.fileSize()).isEqualTo(1024L);
            assertThat(response.expiresInSeconds()).isEqualTo(300);

            verify(storageDownloadService).generateDownloadUrl(
                    "request-submissions",
                    "request-submissions/sample.csv",
                    "sample.csv"
            );
        }

        @Test
        void generateSubmissionDownloadUrl_shouldThrowForbidden_whenBuyerDoesNotOwnRequest() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS, BigDecimal.valueOf(1000));
            AppUser seller = createUser(2L, "seller", Role.SELLER, BigDecimal.valueOf(500));

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    BigDecimal.valueOf(100)
            );

            RequestSubmission submission = createSubmission(
                    99L,
                    request,
                    seller,
                    BigDecimal.valueOf(80),
                    SubmissionStatus.PENDING
            );

            submission.accept(Instant.now());
            request.complete(99L, Instant.now());

            when(requestSubmissionRepo.findById(99L)).thenReturn(Optional.of(submission));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> requestSubmissionService.generateSubmissionDownloadUrl(
                            99L,
                            999L
                    )
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getReason()).isEqualTo("You can only download files from your own accepted request");

            verify(storageDownloadService, never()).generateDownloadUrl(any(), any(), any());
        }
    }

    private AppUser createUser(
            Long id,
            String username,
            Role role,
            BigDecimal credits
    ) {
        Instant now = Instant.now();

        AppUser user = new AppUser(
                username,
                username + "@gmail.com",
                "hashed-password",
                credits,
                role,
                now,
                now
        );

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }

    private DatasetRequest createDatasetRequest(
            Long id,
            AppUser buyer,
            BigDecimal budget
    ) {
        DatasetRequest request = new DatasetRequest(
                buyer,
                Genre.CODING,
                "Need a coding dataset",
                budget,
                Instant.now()
        );

        ReflectionTestUtils.setField(request, "id", id);

        return request;
    }

    private RequestSubmission createSubmission(
            Long id,
            DatasetRequest request,
            AppUser seller,
            BigDecimal proposedPrice,
            SubmissionStatus status
    ) {
        RequestSubmission submission = new RequestSubmission(
                request,
                seller,
                "I can provide this dataset.",
                proposedPrice,
                "sample.csv",
                "text/csv",
                1024L,
                "request-submissions",
                "request-submissions/sample.csv",
                Instant.now()
        );

        ReflectionTestUtils.setField(submission, "id", id);

        if (status == SubmissionStatus.ACCEPTED) {
            submission.accept(Instant.now());
        }

        if (status == SubmissionStatus.REJECTED) {
            submission.reject(Instant.now());
        }

        return submission;
    }

    private MockMultipartFile createFile() {
        return new MockMultipartFile(
                "file",
                "sample.csv",
                "text/csv",
                "id,name\n1,Alice".getBytes()
        );
    }
}