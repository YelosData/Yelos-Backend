package com.yelos.backend.datapost.models;

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
    

    public DataPost(){};

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
        this.aiFlagged = null;
        this.aiFlagDetails = null;
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
    public DataPostStatus getStatus() { return this.status; }
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
        this.version = this.version == null ? 1 : this.version + 1;
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
}
