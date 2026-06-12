package com.yelos.backend.request_marketplace.services;

import com.yelos.backend.request_marketplace.dto.RequestSubmissionDownloadResponse;
import com.yelos.backend.request_marketplace.dto.RequestSubmissionResponse;
import com.yelos.backend.request_marketplace.models.DatasetRequest;
import com.yelos.backend.request_marketplace.models.RequestStatus;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class RequestSubmissionService {

    private final RequestSubmissionRepo requestSubmissionRepo;
    private final DatasetRequestRepo datasetRequestRepo;
    private final UserRepo userRepo;
    private final StorageService storageService;
    private final StorageDownloadService storageDownloadService;

    public RequestSubmissionService(
            RequestSubmissionRepo requestSubmissionRepo,
            DatasetRequestRepo datasetRequestRepo,
            UserRepo userRepo,
            StorageService storageService,
            StorageDownloadService storageDownloadService
    ) {
        this.requestSubmissionRepo = requestSubmissionRepo;
        this.datasetRequestRepo = datasetRequestRepo;
        this.userRepo = userRepo;
        this.storageService = storageService;
        this.storageDownloadService = storageDownloadService;
    }

    @Transactional
    public RequestSubmissionResponse submitToRequest(
            Long requestId,
            Long sellerId,
            String message,
            BigDecimal proposedPrice,
            MultipartFile file
    ) {
        DatasetRequest request = datasetRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dataset request does not exist"
                ));

        AppUser seller = userRepo.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seller does not exist"
                ));

        validateSubmissionRequest(
                request,
                seller,
                message,
                proposedPrice,
                file
        );

        StoredFile storedFile = storageService.uploadDatasetFile(
                seller.getId(),
                file
        );

        RequestSubmission submission = new RequestSubmission(
                request,
                seller,
                message == null ? null : message.trim(),
                proposedPrice,
                storedFile.fileName(),
                storedFile.fileType(),
                storedFile.fileSize(),
                storedFile.bucketName(),
                storedFile.objectKey(),
                Instant.now()
        );

        RequestSubmission savedSubmission = requestSubmissionRepo.save(submission);

        return toResponse(savedSubmission);
    }

    public List<RequestSubmissionResponse> getRequestSubmissions(
            Long requestId,
            Long buyerId
    ) {
        DatasetRequest request = datasetRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dataset request does not exist"
                ));

        if (request.getBuyer().getId() != buyerId) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only view submissions for your own request"
            );
        }

        return requestSubmissionRepo.findByRequestId(requestId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RequestSubmissionResponse> getSellerSubmissions(Long sellerId) {
        AppUser seller = userRepo.findById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seller does not exist"
                ));

        if (seller.getRole() != Role.SELLER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only sellers can view their request submissions"
            );
        }

        return requestSubmissionRepo.findBySellerId(sellerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RequestSubmissionResponse acceptSubmission(
            Long requestId,
            Long submissionId,
            Long buyerId
    ) {
        DatasetRequest request = datasetRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dataset request does not exist"
                ));

        AppUser buyer = userRepo.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Buyer does not exist"
                ));

        if (buyer.getRole() != Role.BUSINESS) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only business users can accept request submissions"
            );
        }

        if (request.getBuyer().getId() != buyerId) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only accept submissions for your own request"
            );
        }

        if (!request.isOpen()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This request is no longer open"
            );
        }

        RequestSubmission submission = requestSubmissionRepo.findByIdAndRequestId(
                submissionId,
                requestId
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Submission does not exist for this request"
        ));

        if (!submission.isPending()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending submissions can be accepted"
            );
        }

        BigDecimal proposedPrice = submission.getProposedPrice();

        if (buyer.getCredits().compareTo(proposedPrice) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Buyer does not have enough credits"
            );
        }

        AppUser seller = submission.getSeller();

        Instant now = Instant.now();

        buyer.deductCred(proposedPrice);
        seller.addCred(proposedPrice);

        submission.accept(now);

        List<RequestSubmission> pendingSubmissions =
                requestSubmissionRepo.findByRequestIdAndStatus(
                        requestId,
                        SubmissionStatus.PENDING
                );

        for (RequestSubmission pendingSubmission : pendingSubmissions) {
            if (!pendingSubmission.getId().equals(submissionId)) {
                pendingSubmission.reject(now);
            }
        }

        request.complete(submissionId, now);

        userRepo.save(buyer);
        userRepo.save(seller);
        requestSubmissionRepo.save(submission);
        requestSubmissionRepo.saveAll(pendingSubmissions);
        datasetRequestRepo.save(request);

        return toResponse(submission);
    }

    public RequestSubmissionDownloadResponse generateSubmissionDownloadUrl(
            Long submissionId,
            Long buyerId
    ) {
        RequestSubmission submission = requestSubmissionRepo.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Submission does not exist"
                ));

        DatasetRequest request = submission.getRequest();

        if (request.getBuyer().getId() != buyerId) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only download files from your own accepted request"
            );
        }

        if (request.getStatus() != RequestStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request is not completed yet"
            );
        }

        if (submission.getStatus() != SubmissionStatus.ACCEPTED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only accepted submissions can be downloaded"
            );
        }

        if (!submission.getId().equals(request.getAcceptedSubmissionId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This submission is not the accepted submission"
            );
        }

        if (submission.getObjectKey() == null || submission.getObjectKey().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Accepted submission does not have a stored file"
            );
        }

        String downloadUrl = storageDownloadService.generateDownloadUrl(
                submission.getBucketName(),
                submission.getObjectKey(),
                submission.getFileName()
        );

        return new RequestSubmissionDownloadResponse(
                downloadUrl,
                submission.getFileName(),
                submission.getFileType(),
                submission.getFileSize(),
                300
        );
    }

    private void validateSubmissionRequest(
            DatasetRequest request,
            AppUser seller,
            String message,
            BigDecimal proposedPrice,
            MultipartFile file
    ) {
        if (!request.isOpen()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This request is no longer open"
            );
        }

        if (seller.getRole() != Role.SELLER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only sellers can submit to dataset requests"
            );
        }

        if (request.getBuyer().getId() == seller.getId()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot submit to your own request"
            );
        }

        boolean alreadySubmitted = requestSubmissionRepo.existsByRequestIdAndSellerId(
                request.getId(),
                seller.getId()
        );

        if (alreadySubmitted) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You have already submitted to this request"
            );
        }

        if (proposedPrice == null || proposedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Proposed price must be greater than 0"
            );
        }

        if (proposedPrice.compareTo(request.getBudget()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Proposed price cannot exceed request budget"
            );
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dataset file is required"
            );
        }
    }

    private RequestSubmissionResponse toResponse(RequestSubmission submission) {
        return new RequestSubmissionResponse(
                submission.getId(),
                submission.getRequest().getId(),
                submission.getSeller().getId(),
                submission.getSeller().getUsername(),
                submission.getMessage(),
                submission.getProposedPrice(),
                submission.getStatus(),
                submission.getFileName(),
                submission.getFileType(),
                submission.getFileSize(),
                submission.getSubmittedAt(),
                submission.getAcceptedAt(),
                submission.getRejectedAt()
        );
    }
}