package com.yelos.backend.request_marketplace.controllers;

import com.yelos.backend.globaldto.PageResponse;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.request_marketplace.dto.DatasetRequestRequest;
import com.yelos.backend.request_marketplace.dto.DatasetRequestResponse;
import com.yelos.backend.request_marketplace.dto.RequestSubmissionDownloadResponse;
import com.yelos.backend.request_marketplace.dto.RequestSubmissionResponse;
import com.yelos.backend.request_marketplace.services.DatasetRequestService;
import com.yelos.backend.request_marketplace.services.RequestSubmissionService;
import com.yelos.backend.security.AuthUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class DatasetRequestController {

    private final DatasetRequestService datasetRequestService;
    private final RequestSubmissionService requestSubmissionService;

    public DatasetRequestController(
            DatasetRequestService datasetRequestService,
            RequestSubmissionService requestSubmissionService
    ) {
        this.datasetRequestService = datasetRequestService;
        this.requestSubmissionService = requestSubmissionService;
    }

    @PostMapping
    public DatasetRequestResponse createRequest(
            @RequestBody DatasetRequestRequest request
    ) {
        return datasetRequestService.createRequest(request);
    }

    @GetMapping("/buyer/{buyerId}")
    public PageResponse<DatasetRequestResponse> getBuyerRequests(
            @PathVariable Long buyerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort);

        Page<DatasetRequestResponse> result = datasetRequestService.getBuyerRequests(
                buyerId,
                pageable
        );

        return toPageResponse(result);
    }

    @GetMapping("/open")
    public PageResponse<DatasetRequestResponse> getOpenRequests(
            @RequestParam(defaultValue = "ALL") String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort);

        Genre parsedGenre = parseGenreFilter(genre);

        Page<DatasetRequestResponse> result = datasetRequestService.getOpenRequests(
                parsedGenre,
                pageable
        );

        return toPageResponse(result);
    }

    @PostMapping(
            value = "/{requestId}/submissions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public RequestSubmissionResponse submitToRequest(
            @PathVariable Long requestId,
            @RequestParam Long sellerId,
            @RequestParam(required = false) String message,
            @RequestParam BigDecimal proposedPrice,
            @RequestParam MultipartFile file
    ) {
        return requestSubmissionService.submitToRequest(
                requestId,
                sellerId,
                message,
                proposedPrice,
                file
        );
    }

    @GetMapping("/{requestId}/submissions")
    public List<RequestSubmissionResponse> getRequestSubmissions(
            @PathVariable Long requestId,
            @RequestParam Long buyerId
    ) {
        return requestSubmissionService.getRequestSubmissions(
                requestId,
                buyerId
        );
    }

    @GetMapping("/seller/{sellerId}/submissions")
    public List<RequestSubmissionResponse> getSellerSubmissions(
            @PathVariable Long sellerId
    ) {
        return requestSubmissionService.getSellerSubmissions(sellerId);
    }

    @PostMapping("/{requestId}/submissions/{submissionId}/accept")
    public RequestSubmissionResponse acceptSubmission(
            @PathVariable Long requestId,
            @PathVariable Long submissionId,
            @RequestParam Long buyerId
    ) {
        return requestSubmissionService.acceptSubmission(
                requestId,
                submissionId,
                buyerId
        );
    }

    @GetMapping("/submissions/{submissionId}/download")
    public RequestSubmissionDownloadResponse downloadAcceptedSubmission(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return requestSubmissionService.generateSubmissionDownloadUrl(
                submissionId,
                principal.userId()
        );
    }

    private Pageable buildPageable(int page, int size, String sort) {
        Sort sortBy = switch (sort) {
            case "oldest" -> Sort.by("createdAt").ascending();
            case "budgetAsc" -> Sort.by("budget").ascending();
            case "budgetDesc" -> Sort.by("budget").descending();
            case "newest" -> Sort.by("createdAt").descending();
            default -> Sort.by("createdAt").descending();
        };

        return PageRequest.of(page, size, sortBy);
    }

    private Genre parseGenreFilter(String genre) {
        if (genre == null || genre.equalsIgnoreCase("ALL")) {
            return null;
        }

        return Genre.valueOf(genre.toUpperCase());
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}