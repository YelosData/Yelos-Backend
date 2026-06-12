package com.yelos.backend.request_marketplace.repo;

import com.yelos.backend.request_marketplace.models.RequestSubmission;
import com.yelos.backend.request_marketplace.models.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequestSubmissionRepo extends JpaRepository<RequestSubmission, Long> {

    List<RequestSubmission> findByRequestId(Long requestId);

    List<RequestSubmission> findBySellerId(Long sellerId);

    List<RequestSubmission> findByRequestIdAndStatus(
            Long requestId,
            SubmissionStatus status
    );

    boolean existsByRequestIdAndSellerId(
            Long requestId,
            Long sellerId
    );

    Optional<RequestSubmission> findByIdAndRequestId(
            Long id,
            Long requestId
    );
}