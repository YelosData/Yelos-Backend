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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class DatasetRequestService {

    private final DatasetRequestRepo datasetRequestRepo;
    private final UserRepo userRepo;

    public DatasetRequestService(
            DatasetRequestRepo datasetRequestRepo,
            UserRepo userRepo
    ) {
        this.datasetRequestRepo = datasetRequestRepo;
        this.userRepo = userRepo;
    }

    public DatasetRequestResponse createRequest(DatasetRequestRequest request) {
        AppUser buyer = userRepo.findById(request.buyerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Buyer does not exist"
                ));

        if (buyer.getRole() != Role.BUSINESS) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only business users can create dataset requests"
            );
        }

        validateCreateRequest(request);

        Instant now = Instant.now();

        DatasetRequest datasetRequest = new DatasetRequest(
                buyer,
                request.genre(),
                request.specification().trim(),
                request.budget(),
                now
        );

        DatasetRequest savedRequest = datasetRequestRepo.save(datasetRequest);

        return toResponse(savedRequest);
    }

    public Page<DatasetRequestResponse> getBuyerRequests(
            Long buyerId,
            Pageable pageable
    ) {
        AppUser buyer = userRepo.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Buyer does not exist"
                ));

        if (buyer.getRole() != Role.BUSINESS) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only business users can view their dataset requests"
            );
        }

        return datasetRequestRepo.findByBuyerId(buyerId, pageable)
                .map(this::toResponse);
    }

    public Page<DatasetRequestResponse> getOpenRequests(
            Genre genre,
            Pageable pageable
    ) {
        Page<DatasetRequest> requests;

        if (genre == null) {
            requests = datasetRequestRepo.findByStatus(
                    RequestStatus.OPEN,
                    pageable
            );
        } else {
            requests = datasetRequestRepo.findByGenreAndStatus(
                    genre,
                    RequestStatus.OPEN,
                    pageable
            );
        }

        return requests.map(this::toResponse);
    }

    private void validateCreateRequest(DatasetRequestRequest request) {
        if (request.buyerId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Buyer id is required"
            );
        }

        if (request.genre() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Genre is required"
            );
        }

        if (request.specification() == null || request.specification().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Specification is required"
            );
        }

        if (request.budget() == null || request.budget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Budget must be greater than 0"
            );
        }
    }

    private DatasetRequestResponse toResponse(DatasetRequest request) {
        return new DatasetRequestResponse(
                request.getId(),
                request.getBuyer().getId(),
                request.getBuyer().getUsername(),
                request.getGenre(),
                request.getSpecification(),
                request.getBudget(),
                request.getStatus(),
                request.getAcceptedSubmissionId(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getCompletedAt()
        );
    }
}