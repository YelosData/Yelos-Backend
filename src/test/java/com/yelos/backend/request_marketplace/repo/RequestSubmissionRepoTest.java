package com.yelos.backend.request_marketplace.repo;

import com.yelos.backend.IntegrationTestBase;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.request_marketplace.models.DatasetRequest;
import com.yelos.backend.request_marketplace.models.RequestStatus;
import com.yelos.backend.request_marketplace.models.RequestSubmission;
import com.yelos.backend.request_marketplace.models.SubmissionStatus;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RequestSubmissionRepoTest extends IntegrationTestBase {

    @Nested
    class FindByRequestIdTests {

        @Test
        void findByRequestId_shouldReturnOnlySubmissionsForThatRequest() {
            AppUser buyer = createUser("buyer", "buyer@gmail.com", Role.BUSINESS);
            AppUser seller1 = createUser("seller1", "seller1@gmail.com", Role.SELLER);
            AppUser seller2 = createUser("seller2", "seller2@gmail.com", Role.SELLER);

            DatasetRequest request1 = createDatasetRequest(buyer, "Request 1");
            DatasetRequest request2 = createDatasetRequest(buyer, "Request 2");

            RequestSubmission submission1 = createSubmission(
                    request1,
                    seller1,
                    SubmissionStatus.PENDING
            );

            RequestSubmission submission2 = createSubmission(
                    request1,
                    seller2,
                    SubmissionStatus.PENDING
            );

            createSubmission(
                    request2,
                    seller1,
                    SubmissionStatus.PENDING
            );

            List<RequestSubmission> result = requestSubmissionRepo.findByRequestId(
                    request1.getId()
            );

            assertThat(result)
                    .extracting(RequestSubmission::getId)
                    .containsExactlyInAnyOrder(
                            submission1.getId(),
                            submission2.getId()
                    );
        }
    }

    @Nested
    class FindBySellerIdTests {

        @Test
        void findBySellerId_shouldReturnOnlySubmissionsCreatedByThatSeller() {
            AppUser buyer = createUser("buyer", "buyer@gmail.com", Role.BUSINESS);
            AppUser seller1 = createUser("seller1", "seller1@gmail.com", Role.SELLER);
            AppUser seller2 = createUser("seller2", "seller2@gmail.com", Role.SELLER);

            DatasetRequest request1 = createDatasetRequest(buyer, "Request 1");
            DatasetRequest request2 = createDatasetRequest(buyer, "Request 2");

            RequestSubmission seller1Submission1 = createSubmission(
                    request1,
                    seller1,
                    SubmissionStatus.PENDING
            );

            RequestSubmission seller1Submission2 = createSubmission(
                    request2,
                    seller1,
                    SubmissionStatus.ACCEPTED
            );

            createSubmission(
                    request1,
                    seller2,
                    SubmissionStatus.PENDING
            );

            List<RequestSubmission> result = requestSubmissionRepo.findBySellerId(
                    seller1.getId()
            );

            assertThat(result)
                    .extracting(RequestSubmission::getId)
                    .containsExactlyInAnyOrder(
                            seller1Submission1.getId(),
                            seller1Submission2.getId()
                    );
        }
    }

    @Nested
    class FindByRequestIdAndStatusTests {

        @Test
        void findByRequestIdAndStatus_shouldReturnOnlyMatchingRequestAndStatus() {
            AppUser buyer = createUser("buyer", "buyer@gmail.com", Role.BUSINESS);
            AppUser seller1 = createUser("seller1", "seller1@gmail.com", Role.SELLER);
            AppUser seller2 = createUser("seller2", "seller2@gmail.com", Role.SELLER);
            AppUser seller3 = createUser("seller3", "seller3@gmail.com", Role.SELLER);

            DatasetRequest request = createDatasetRequest(buyer, "Request 1");

            RequestSubmission pendingSubmission1 = createSubmission(
                    request,
                    seller1,
                    SubmissionStatus.PENDING
            );

            RequestSubmission pendingSubmission2 = createSubmission(
                    request,
                    seller2,
                    SubmissionStatus.PENDING
            );

            createSubmission(
                    request,
                    seller3,
                    SubmissionStatus.ACCEPTED
            );

            List<RequestSubmission> result = requestSubmissionRepo.findByRequestIdAndStatus(
                    request.getId(),
                    SubmissionStatus.PENDING
            );

            assertThat(result)
                    .extracting(RequestSubmission::getId)
                    .containsExactlyInAnyOrder(
                            pendingSubmission1.getId(),
                            pendingSubmission2.getId()
                    );
        }
    }

    @Nested
    class ExistsByRequestIdAndSellerIdTests {

        @Test
        void existsByRequestIdAndSellerId_shouldReturnTrueWhenSellerSubmittedToRequest() {
            AppUser buyer = createUser("buyer", "buyer@gmail.com", Role.BUSINESS);
            AppUser seller = createUser("seller", "seller@gmail.com", Role.SELLER);

            DatasetRequest request = createDatasetRequest(buyer, "Request 1");

            createSubmission(
                    request,
                    seller,
                    SubmissionStatus.PENDING
            );

            boolean result = requestSubmissionRepo.existsByRequestIdAndSellerId(
                    request.getId(),
                    seller.getId()
            );

            assertThat(result).isTrue();
        }

        @Test
        void existsByRequestIdAndSellerId_shouldReturnFalseWhenSellerDidNotSubmitToRequest() {
            AppUser buyer = createUser("buyer", "buyer@gmail.com", Role.BUSINESS);
            AppUser seller = createUser("seller", "seller@gmail.com", Role.SELLER);

            DatasetRequest request = createDatasetRequest(buyer, "Request 1");

            boolean result = requestSubmissionRepo.existsByRequestIdAndSellerId(
                    request.getId(),
                    seller.getId()
            );

            assertThat(result).isFalse();
        }
    }

    @Nested
    class FindByIdAndRequestIdTests {

        @Test
        void findByIdAndRequestId_shouldReturnSubmissionWhenBothMatch() {
            AppUser buyer = createUser("buyer", "buyer@gmail.com", Role.BUSINESS);
            AppUser seller = createUser("seller", "seller@gmail.com", Role.SELLER);

            DatasetRequest request = createDatasetRequest(buyer, "Request 1");

            RequestSubmission submission = createSubmission(
                    request,
                    seller,
                    SubmissionStatus.PENDING
            );

            Optional<RequestSubmission> result = requestSubmissionRepo.findByIdAndRequestId(
                    submission.getId(),
                    request.getId()
            );

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(submission.getId());
        }

        @Test
        void findByIdAndRequestId_shouldReturnEmptyWhenRequestDoesNotMatch() {
            AppUser buyer = createUser("buyer", "buyer@gmail.com", Role.BUSINESS);
            AppUser seller = createUser("seller", "seller@gmail.com", Role.SELLER);

            DatasetRequest request1 = createDatasetRequest(buyer, "Request 1");
            DatasetRequest request2 = createDatasetRequest(buyer, "Request 2");

            RequestSubmission submission = createSubmission(
                    request1,
                    seller,
                    SubmissionStatus.PENDING
            );

            Optional<RequestSubmission> result = requestSubmissionRepo.findByIdAndRequestId(
                    submission.getId(),
                    request2.getId()
            );

            assertThat(result).isEmpty();
        }
    }

    private AppUser createUser(String username, String email, Role role) {
        Instant now = Instant.now();

        AppUser user = new AppUser(
                username,
                email,
                "hashed-password",
                BigDecimal.valueOf(1000),
                role,
                now,
                now
        );

        return userRepo.save(user);
    }

    private DatasetRequest createDatasetRequest(AppUser buyer, String specification) {
        Instant now = Instant.now();

        DatasetRequest request = new DatasetRequest(
                buyer,
                Genre.CODING,
                specification,
                BigDecimal.valueOf(100),
                now
        );

        return datasetRequestRepo.save(request);
    }

    private RequestSubmission createSubmission(
            DatasetRequest request,
            AppUser seller,
            SubmissionStatus status
    ) {
        Instant now = Instant.now();

        RequestSubmission submission = new RequestSubmission(
                request,
                seller,
                "I can provide this dataset.",
                BigDecimal.valueOf(80),
                "sample.csv",
                "text/csv",
                1024L,
                "request-submissions",
                "request-submissions/sample.csv",
                now
        );

        if (status == SubmissionStatus.ACCEPTED) {
            submission.accept(now.plusSeconds(60));
        }

        if (status == SubmissionStatus.REJECTED) {
            submission.reject(now.plusSeconds(60));
        }

        return requestSubmissionRepo.save(submission);
    }
}