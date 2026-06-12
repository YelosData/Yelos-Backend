package com.yelos.backend.request_marketplace.models;

import com.yelos.backend.user.models.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "request_submissions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"request_id", "seller_id"})
        }
)
public class RequestSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private DatasetRequest request;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private AppUser seller;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private BigDecimal proposedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String bucketName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private Instant submittedAt;

    private Instant acceptedAt;

    private Instant rejectedAt;

    public RequestSubmission() {}

    public RequestSubmission(
            DatasetRequest request,
            AppUser seller,
            String message,
            BigDecimal proposedPrice,
            String fileName,
            String fileType,
            Long fileSize,
            String bucketName,
            String objectKey,
            Instant submittedAt
    ) {
        this.request = request;
        this.seller = seller;
        this.message = message;
        this.proposedPrice = proposedPrice;
        this.status = SubmissionStatus.PENDING;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.submittedAt = submittedAt;
    }

    public void accept(Instant acceptedAt) {
        this.status = SubmissionStatus.ACCEPTED;
        this.acceptedAt = acceptedAt;
    }

    public void reject(Instant rejectedAt) {
        this.status = SubmissionStatus.REJECTED;
        this.rejectedAt = rejectedAt;
    }

    public boolean isPending() {
        return this.status == SubmissionStatus.PENDING;
    }

    public Long getId() {
        return this.id;
    }

    public DatasetRequest getRequest() {
        return this.request;
    }

    public AppUser getSeller() {
        return this.seller;
    }

    public String getMessage() {
        return this.message;
    }

    public BigDecimal getProposedPrice() {
        return this.proposedPrice;
    }

    public SubmissionStatus getStatus() {
        return this.status;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getFileType() {
        return this.fileType;
    }

    public Long getFileSize() {
        return this.fileSize;
    }

    public String getBucketName() {
        return this.bucketName;
    }

    public String getObjectKey() {
        return this.objectKey;
    }

    public Instant getSubmittedAt() {
        return this.submittedAt;
    }

    public Instant getAcceptedAt() {
        return this.acceptedAt;
    }

    public Instant getRejectedAt() {
        return this.rejectedAt;
    }
}