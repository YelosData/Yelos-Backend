package com.yelos.backend.request_marketplace.repo;

import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.request_marketplace.models.DatasetRequest;
import com.yelos.backend.request_marketplace.models.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRequestRepo extends JpaRepository<DatasetRequest, Long> {

    Page<DatasetRequest> findByBuyerId(Long buyerId, Pageable pageable);

    Page<DatasetRequest> findByStatus(RequestStatus status, Pageable pageable);

    Page<DatasetRequest> findByGenreAndStatus(
            Genre genre,
            RequestStatus status,
            Pageable pageable
    );
}