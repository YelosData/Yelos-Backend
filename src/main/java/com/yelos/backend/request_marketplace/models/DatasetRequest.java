package com.yelos.backend.request_marketplace.models;

import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.user.models.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "dataset_requests")
public class DatasetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private AppUser buyer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genre genre;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String specification;

    @Column(nullable = false)
    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    private Long acceptedSubmissionId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant completedAt;

    public DatasetRequest() {}

    public DatasetRequest(
            AppUser buyer,
            Genre genre,
            String specification,
            BigDecimal budget,
            Instant createdAt
    ) {
        this.buyer = buyer;
        this.genre = genre;
        this.specification = specification;
        this.budget = budget;
        this.status = RequestStatus.OPEN;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void complete(Long acceptedSubmissionId, Instant completedAt) {
        this.status = RequestStatus.COMPLETED;
        this.acceptedSubmissionId = acceptedSubmissionId;
        this.completedAt = completedAt;
        this.updatedAt = completedAt;
    }

    public void cancel(Instant cancelledAt) {
        this.status = RequestStatus.CANCELLED;
        this.updatedAt = cancelledAt;
    }

    public boolean isOpen() {
        return this.status == RequestStatus.OPEN;
    }

    public Long getId() {
        return this.id;
    }

    public AppUser getBuyer() {
        return this.buyer;
    }

    public Genre getGenre() {
        return this.genre;
    }

    public String getSpecification() {
        return this.specification;
    }

    public BigDecimal getBudget() {
        return this.budget;
    }

    public RequestStatus getStatus() {
        return this.status;
    }

    public Long getAcceptedSubmissionId() {
        return this.acceptedSubmissionId;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Instant getCompletedAt() {
        return this.completedAt;
    }
}