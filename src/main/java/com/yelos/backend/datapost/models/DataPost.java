package com.yelos.backend.datapost.models;

import com.yelos.backend.storage.dto.StoredFile;
import com.yelos.backend.user.models.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

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

    public DataPost(){};

    public DataPost(
            AppUser user,
            Genre genre,
            String title,
            String summary,
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
        this.summary = summary;
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
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }

        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }

        if (this.status == null) {
            this.status = DataPostStatus.ACTIVE;
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
}
