package com.yelos.backend.datapost.models;

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

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 1000, nullable = false)
    private String summary;
    private BigDecimal price;
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private DataPostStatus status;

    public DataPost(){};

    public DataPost(
            AppUser user,
            Genre genre,
            String title,
            String content,
            String summary,
            BigDecimal price,
            DataPostStatus status,
            Instant createdAt
    ) {
        this.user = user;
        this.genre = genre;
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
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
}
