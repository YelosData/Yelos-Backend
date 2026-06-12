package com.yelos.backend.request_marketplace.repo;

import com.yelos.backend.IntegrationTestBase;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.request_marketplace.models.DatasetRequest;
import com.yelos.backend.request_marketplace.models.RequestStatus;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetRequestRepoTest extends IntegrationTestBase {

    @Nested
    class FindByBuyerIdTests {

        @Test
        void findByBuyerId_shouldReturnOnlyRequestsCreatedByThatBuyer() {
            AppUser buyer1 = createUser("buyer1", "buyer1@gmail.com", Role.BUSINESS);
            AppUser buyer2 = createUser("buyer2", "buyer2@gmail.com", Role.BUSINESS);

            DatasetRequest buyer1Request1 = createDatasetRequest(
                    buyer1,
                    "Need coding dataset",
                    Genre.CODING,
                    RequestStatus.OPEN
            );

            DatasetRequest buyer1Request2 = createDatasetRequest(
                    buyer1,
                    "Need finance dataset",
                    Genre.FINANCE,
                    RequestStatus.OPEN
            );

            createDatasetRequest(
                    buyer2,
                    "Need travel dataset",
                    Genre.TRAVEL,
                    RequestStatus.OPEN
            );

            Page<DatasetRequest> result = datasetRequestRepo.findByBuyerId(
                    buyer1.getId(),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(DatasetRequest::getId)
                    .containsExactlyInAnyOrder(
                            buyer1Request1.getId(),
                            buyer1Request2.getId()
                    );

            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        void findByBuyerId_shouldReturnEmptyPageWhenBuyerHasNoRequests() {
            AppUser buyer1 = createUser("buyer1", "buyer1@gmail.com", Role.BUSINESS);
            AppUser buyer2 = createUser("buyer2", "buyer2@gmail.com", Role.BUSINESS);

            createDatasetRequest(
                    buyer1,
                    "Need coding dataset",
                    Genre.CODING,
                    RequestStatus.OPEN
            );

            Page<DatasetRequest> result = datasetRequestRepo.findByBuyerId(
                    buyer2.getId(),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    class FindByStatusTests {

        @Test
        void findByStatus_shouldReturnOnlyRequestsWithMatchingStatus() {
            AppUser buyer = createUser("buyer", "buyer@gmail.com", Role.BUSINESS);

            DatasetRequest openRequest1 = createDatasetRequest(
                    buyer,
                    "Open request 1",
                    Genre.CODING,
                    RequestStatus.OPEN
            );

            DatasetRequest openRequest2 = createDatasetRequest(
                    buyer,
                    "Open request 2",
                    Genre.FINANCE,
                    RequestStatus.OPEN
            );

            createDatasetRequest(
                    buyer,
                    "Completed request",
                    Genre.CODING,
                    RequestStatus.COMPLETED
            );

            Page<DatasetRequest> result = datasetRequestRepo.findByStatus(
                    RequestStatus.OPEN,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(DatasetRequest::getId)
                    .containsExactlyInAnyOrder(
                            openRequest1.getId(),
                            openRequest2.getId()
                    );

            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }

    @Nested
    class FindByGenreAndStatusTests {

        @Test
        void findByGenreAndStatus_shouldReturnOnlyRequestsMatchingGenreAndStatus() {
            AppUser buyer = createUser("buyer", "buyer@gmail.com", Role.BUSINESS);

            DatasetRequest matchingRequest = createDatasetRequest(
                    buyer,
                    "Open coding request",
                    Genre.CODING,
                    RequestStatus.OPEN
            );

            createDatasetRequest(
                    buyer,
                    "Completed coding request",
                    Genre.CODING,
                    RequestStatus.COMPLETED
            );

            createDatasetRequest(
                    buyer,
                    "Open finance request",
                    Genre.FINANCE,
                    RequestStatus.OPEN
            );

            Page<DatasetRequest> result = datasetRequestRepo.findByGenreAndStatus(
                    Genre.CODING,
                    RequestStatus.OPEN,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(DatasetRequest::getId)
                    .containsExactly(matchingRequest.getId());

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void findByGenreAndStatus_shouldReturnEmptyPageWhenNoRequestMatches() {
            AppUser buyer = createUser("buyer", "buyer@gmail.com", Role.BUSINESS);

            createDatasetRequest(
                    buyer,
                    "Open coding request",
                    Genre.CODING,
                    RequestStatus.OPEN
            );

            Page<DatasetRequest> result = datasetRequestRepo.findByGenreAndStatus(
                    Genre.FINANCE,
                    RequestStatus.OPEN,
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
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

    private DatasetRequest createDatasetRequest(
            AppUser buyer,
            String specification,
            Genre genre,
            RequestStatus status
    ) {
        Instant now = Instant.now();

        DatasetRequest request = new DatasetRequest(
                buyer,
                genre,
                specification,
                BigDecimal.valueOf(100),
                now
        );

        if (status == RequestStatus.COMPLETED) {
            request.complete(999L, now.plusSeconds(60));
        }

        if (status == RequestStatus.CANCELLED) {
            request.cancel(now.plusSeconds(60));
        }

        return datasetRequestRepo.save(request);
    }
}