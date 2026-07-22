package com.yelos.backend.datapost.models;

import com.yelos.backend.storage.dto.StoredFile;
import com.yelos.backend.user.models.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.yelos.backend.storage.dto.StoredFile;
import com.yelos.backend.user.models.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;


@Entity
@Table(name = "user_posts")
public class DataPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    private Genre genre;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content; // Legacy, we are now migrating to use buckets to store files

    @Column(length = 1000, nullable = false)
    private String summary;
    private BigDecimal price;
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private DataPostStatus status;
    private Instant updatedAt;
    private Instant deletedAt;
    private Integer version;

    private String fileName;
    private String fileType;
    private Long fileSize;

    private String bucketName;

    @Column(nullable = false, unique = true)
    private String objectKey;

    private Boolean adminReviewed;

    private Instant reviewedAt;

    private Long reviewedByAdminId;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @Column(columnDefinition = "TEXT")
    private String previewCsv;

    private Boolean aiFlagged;

    @Column(columnDefinition = "TEXT")
    private String aiFlagDetails;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "real[]")
    private float[] embedding;

    // Dataset quality rating fields

    @Column(name = "quality_completeness", precision = 5, scale = 2)
    private BigDecimal qualityCompleteness;

    @Column(name = "quality_consistency", precision = 5, scale = 2)
    private BigDecimal qualityConsistency;

    @Column(name = "quality_uniqueness", precision = 5, scale = 2)
    private BigDecimal qualityUniqueness;

    @Column(name = "quality_validity", precision = 5, scale = 2)
    private BigDecimal qualityValidity;

    @Column(name = "quality_richness", precision = 5, scale = 2)
    private BigDecimal qualityRichness;

    @Column(name = "quality_total", precision = 5, scale = 2)
    private BigDecimal qualityTotal;


    public DataPost() {
    }

    public DataPost(
            AppUser user,
            Genre genre,
            String title,
            String aiSummary,
            BigDecimal price,
            DataPostStatus status,
            Instant createdAt,
            String fileName,
            String fileType,
            Long fileSize,
            String bucketName,
            String objectKey
    ) {
        this.user = user;
        this.genre = genre;
        this.title = title;
        this.summary = aiSummary;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.deletedAt = null;
        this.version = 1;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.adminReviewed = false;
        this.reviewedAt = null;
        this.reviewedByAdminId = null;
        this.adminNote = null;
        this.previewCsv = null;
        this.aiFlagged = false;
        this.aiFlagDetails = null;
        this.embedding = null;

        this.qualityCompleteness = null;
        this.qualityConsistency = null;
        this.qualityUniqueness = null;
        this.qualityValidity = null;
        this.qualityRichness = null;
        this.qualityTotal = null;
    }

    public long getId() {
        return this.id;
    }

    public AppUser getUser() {
        return this.user;
    }

    public Genre getGenre() {
        return this.genre;
    }
    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }
    public String getSummary() {
        return this.summary;
    }
    public BigDecimal getPrice() {
        return this.price;
    }

    public DataPostStatus getStatus() {
        return this.status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
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

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Instant getDeletedAt() {
        return this.deletedAt;
    }

    public Integer getVersion() {
        return this.version;
    }

    public Boolean getAdminReviewed() {
        return this.adminReviewed;
    }

    public Instant getReviewedAt() {
        return this.reviewedAt;
    }

    public Long getReviewedByAdminId() {
        return this.reviewedByAdminId;
    }

    public String getAdminNote() {
        return this.adminNote;
    }

    public String getPreviewCsv() {
        return this.previewCsv;
    }

    public Boolean getAiFlagged() {
        return this.aiFlagged;
    }

    public String getAiFlagDetails() {
        return this.aiFlagDetails;
    }

    public float[] getEmbedding() {
        return this.embedding;
    }

    public BigDecimal getQualityCompleteness() {
        return this.qualityCompleteness;
    }

    public BigDecimal getQualityConsistency() {
        return this.qualityConsistency;
    }

    public BigDecimal getQualityUniqueness() {
        return this.qualityUniqueness;
    }

    public BigDecimal getQualityValidity() {
        return this.qualityValidity;
    }

    public BigDecimal getQualityRichness() {
        return this.qualityRichness;
    }

    public BigDecimal getQualityTotal() {
        return this.qualityTotal;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public void attachAiMetadata(
            String aiSummary,
            String previewCsv,
            Boolean aiFlagged,
            String aiFlagDetails
    ) {
        if (aiSummary != null && !aiSummary.isBlank()) {
            this.summary = aiSummary;
        }

        this.previewCsv = previewCsv;
        this.aiFlagged = aiFlagged;
        this.aiFlagDetails = aiFlagDetails;
    }

    public void attachQualityRating(
            BigDecimal completeness,
            BigDecimal consistency,
            BigDecimal uniqueness,
            BigDecimal validity,
            BigDecimal richness,
            BigDecimal total
    ) {
        this.qualityCompleteness = completeness;
        this.qualityConsistency = consistency;
        this.qualityUniqueness = uniqueness;
        this.qualityValidity = validity;
        this.qualityRichness = richness;
        this.qualityTotal = total;
    }

    @PrePersist
    public void prePersist() {              // For legacy entries whose fields are not valid and updated
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }

        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }

        if (this.status == null) {
            this.status = DataPostStatus.ACTIVE;
        }

        if (this.adminReviewed == null) {
            this.adminReviewed = false;
        }

        if (this.version == null) {
            this.version = 1;
        }

        if (this.aiFlagged == null) {
            this.aiFlagged = false;
        }
    }

    public void updatePost(
            Genre genre,
            String title,
            String summary,
            BigDecimal price,
            StoredFile storedFile,
            Instant now
    ) {
        this.genre = genre;
        this.title = title;
        this.summary = summary;
        this.price = price;

        if (storedFile != null) {
            this.fileName = storedFile.fileName();
            this.fileType = storedFile.fileType();
            this.fileSize = storedFile.fileSize();
            this.bucketName = storedFile.bucketName();
            this.objectKey = storedFile.objectKey();

            incrementVersion();
        }

        this.updatedAt = now;
    }

    public void markDeleted(Instant now) {
        this.status = DataPostStatus.DELETED;
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public boolean isDeleted() {
        return this.status == DataPostStatus.DELETED;
    }

    private void incrementVersion() {
        this.version = this.version == null
                ? 1
                : this.version + 1;
    }

    public void markReviewed(Long adminId, String note, Instant now) {
        this.adminReviewed = true;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
        this.adminNote = note;
        this.updatedAt = now;
    }

    public void hideByAdmin(Long adminId, String note, Instant now) {
        this.status = DataPostStatus.DELETED;
        this.adminReviewed = true;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
        this.adminNote = note;
        this.updatedAt = now;
    }

    public void markAiProcessing(Instant now) {
        this.status = DataPostStatus.PROCESSING;
        this.updatedAt = now;
    }

    public void markAiCompleted(
            String aiSummary,
            String previewCsv,
            Boolean aiFlagged,
            String aiFlagDetails,
            Instant now
    ) {
        attachAiMetadata(
                aiSummary,
                previewCsv,
                aiFlagged,
                aiFlagDetails
        );

        this.status = DataPostStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void completeAiProcessingWithStoredFile(
            String aiSummary,
            String previewCsv,
            Boolean aiFlagged,
            String aiFlagDetails,
            StoredFile storedFile,
            Instant now
    ) {
        attachAiMetadata(
                aiSummary,
                previewCsv,
                aiFlagged,
                aiFlagDetails
        );

        if (storedFile != null) {
            this.fileName = storedFile.fileName();
            this.fileType = storedFile.fileType();
            this.fileSize = storedFile.fileSize();
            this.bucketName = storedFile.bucketName();
            this.objectKey = storedFile.objectKey();
        }

        this.status = DataPostStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void markAiFailed(String errorMessage, Instant now) {
        this.status = DataPostStatus.AI_FAILED;
        this.aiFlagged = true;
        this.aiFlagDetails = errorMessage;
        this.updatedAt = now;
    }
}
