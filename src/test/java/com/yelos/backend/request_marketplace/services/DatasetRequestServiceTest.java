package com.yelos.backend.request_marketplace.services;

import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.request_marketplace.dto.DatasetRequestRequest;
import com.yelos.backend.request_marketplace.dto.DatasetRequestResponse;
import com.yelos.backend.request_marketplace.models.DatasetRequest;
import com.yelos.backend.request_marketplace.models.RequestStatus;
import com.yelos.backend.request_marketplace.repo.DatasetRequestRepo;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatasetRequestServiceTest {

    @Mock
    private DatasetRequestRepo datasetRequestRepo;

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private DatasetRequestService datasetRequestService;

    @Nested
    class CreateRequestTests {

        @Test
        void createRequest_shouldCreateDatasetRequest_whenBuyerIsBusinessAndRequestIsValid() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS);

            DatasetRequestRequest request = new DatasetRequestRequest(
                    1L,
                    Genre.CODING,
                    "   Need Java coding interview dataset   ",
                    BigDecimal.valueOf(100)
            );

            when(userRepo.findById(1L)).thenReturn(Optional.of(buyer));

            when(datasetRequestRepo.save(any(DatasetRequest.class)))
                    .thenAnswer(invocation -> {
                        DatasetRequest savedRequest = invocation.getArgument(0);
                        ReflectionTestUtils.setField(savedRequest, "id", 10L);
                        return savedRequest;
                    });

            DatasetRequestResponse response = datasetRequestService.createRequest(request);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.buyerId()).isEqualTo(1L);
            assertThat(response.buyerUsername()).isEqualTo("buyer");
            assertThat(response.genre()).isEqualTo(Genre.CODING);
            assertThat(response.specification()).isEqualTo("Need Java coding interview dataset");
            assertThat(response.budget()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(response.status()).isEqualTo(RequestStatus.OPEN);
            assertThat(response.acceptedSubmissionId()).isNull();
            assertThat(response.completedAt()).isNull();

            verify(userRepo).findById(1L);
            verify(datasetRequestRepo).save(any(DatasetRequest.class));
        }

        @Test
        void createRequest_shouldThrowNotFound_whenBuyerDoesNotExist() {
            DatasetRequestRequest request = new DatasetRequestRequest(
                    1L,
                    Genre.CODING,
                    "Need dataset",
                    BigDecimal.valueOf(100)
            );

            when(userRepo.findById(1L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> datasetRequestService.createRequest(request)
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getReason()).isEqualTo("Buyer does not exist");

            verify(datasetRequestRepo, never()).save(any());
        }

        @Test
        void createRequest_shouldThrowForbidden_whenUserIsNotBusiness() {
            AppUser seller = createUser(1L, "seller", Role.SELLER);

            DatasetRequestRequest request = new DatasetRequestRequest(
                    1L,
                    Genre.CODING,
                    "Need dataset",
                    BigDecimal.valueOf(100)
            );

            when(userRepo.findById(1L)).thenReturn(Optional.of(seller));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> datasetRequestService.createRequest(request)
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getReason()).isEqualTo("Only business users can create dataset requests");

            verify(datasetRequestRepo, never()).save(any());
        }

        @Test
        void createRequest_shouldThrowBadRequest_whenGenreIsNull() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS);

            DatasetRequestRequest request = new DatasetRequestRequest(
                    1L,
                    null,
                    "Need dataset",
                    BigDecimal.valueOf(100)
            );

            when(userRepo.findById(1L)).thenReturn(Optional.of(buyer));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> datasetRequestService.createRequest(request)
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).isEqualTo("Genre is required");

            verify(datasetRequestRepo, never()).save(any());
        }

        @Test
        void createRequest_shouldThrowBadRequest_whenSpecificationIsBlank() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS);

            DatasetRequestRequest request = new DatasetRequestRequest(
                    1L,
                    Genre.CODING,
                    "   ",
                    BigDecimal.valueOf(100)
            );

            when(userRepo.findById(1L)).thenReturn(Optional.of(buyer));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> datasetRequestService.createRequest(request)
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).isEqualTo("Specification is required");

            verify(datasetRequestRepo, never()).save(any());
        }

        @Test
        void createRequest_shouldThrowBadRequest_whenBudgetIsZeroOrNegative() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS);

            DatasetRequestRequest request = new DatasetRequestRequest(
                    1L,
                    Genre.CODING,
                    "Need dataset",
                    BigDecimal.ZERO
            );

            when(userRepo.findById(1L)).thenReturn(Optional.of(buyer));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> datasetRequestService.createRequest(request)
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).isEqualTo("Budget must be greater than 0");

            verify(datasetRequestRepo, never()).save(any());
        }
    }

    @Nested
    class GetBuyerRequestsTests {

        @Test
        void getBuyerRequests_shouldReturnBuyerRequests_whenBuyerIsBusiness() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS);

            DatasetRequest request1 = createDatasetRequest(
                    10L,
                    buyer,
                    Genre.CODING,
                    "Need coding dataset",
                    BigDecimal.valueOf(100)
            );

            DatasetRequest request2 = createDatasetRequest(
                    11L,
                    buyer,
                    Genre.FINANCE,
                    "Need finance dataset",
                    BigDecimal.valueOf(200)
            );

            PageRequest pageable = PageRequest.of(0, 10);

            when(userRepo.findById(1L)).thenReturn(Optional.of(buyer));
            when(datasetRequestRepo.findByBuyerId(1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(request1, request2)));

            Page<DatasetRequestResponse> result = datasetRequestService.getBuyerRequests(
                    1L,
                    pageable
            );

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                    .extracting(DatasetRequestResponse::id)
                    .containsExactlyInAnyOrder(10L, 11L);

            verify(datasetRequestRepo).findByBuyerId(1L, pageable);
        }

        @Test
        void getBuyerRequests_shouldThrowForbidden_whenUserIsNotBusiness() {
            AppUser seller = createUser(1L, "seller", Role.SELLER);

            when(userRepo.findById(1L)).thenReturn(Optional.of(seller));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> datasetRequestService.getBuyerRequests(
                            1L,
                            PageRequest.of(0, 10)
                    )
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getReason()).isEqualTo("Only business users can view their dataset requests");

            verify(datasetRequestRepo, never()).findByBuyerId(anyLong(), any());
        }
    }

    @Nested
    class GetOpenRequestsTests {

        @Test
        void getOpenRequests_shouldFindByStatus_whenGenreIsNull() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS);

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    Genre.CODING,
                    "Need coding dataset",
                    BigDecimal.valueOf(100)
            );

            PageRequest pageable = PageRequest.of(0, 10);

            when(datasetRequestRepo.findByStatus(RequestStatus.OPEN, pageable))
                    .thenReturn(new PageImpl<>(List.of(request)));

            Page<DatasetRequestResponse> result = datasetRequestService.getOpenRequests(
                    null,
                    pageable
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(10L);

            verify(datasetRequestRepo).findByStatus(RequestStatus.OPEN, pageable);
            verify(datasetRequestRepo, never()).findByGenreAndStatus(any(), any(), any());
        }

        @Test
        void getOpenRequests_shouldFindByGenreAndStatus_whenGenreIsProvided() {
            AppUser buyer = createUser(1L, "buyer", Role.BUSINESS);

            DatasetRequest request = createDatasetRequest(
                    10L,
                    buyer,
                    Genre.CODING,
                    "Need coding dataset",
                    BigDecimal.valueOf(100)
            );

            PageRequest pageable = PageRequest.of(0, 10);

            when(datasetRequestRepo.findByGenreAndStatus(
                    Genre.CODING,
                    RequestStatus.OPEN,
                    pageable
            )).thenReturn(new PageImpl<>(List.of(request)));

            Page<DatasetRequestResponse> result = datasetRequestService.getOpenRequests(
                    Genre.CODING,
                    pageable
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).genre()).isEqualTo(Genre.CODING);

            verify(datasetRequestRepo).findByGenreAndStatus(
                    Genre.CODING,
                    RequestStatus.OPEN,
                    pageable
            );
            verify(datasetRequestRepo, never()).findByStatus(any(), any());
        }
    }

    private AppUser createUser(Long id, String username, Role role) {
        Instant now = Instant.now();

        AppUser user = new AppUser(
                username,
                username + "@gmail.com",
                "hashed-password",
                BigDecimal.valueOf(1000),
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

        ReflectionTestUtils.setField(request, "id", id);

        return request;
    }
}