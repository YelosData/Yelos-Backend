package com.yelos.backend.purchase.models;

import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.user.models.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "purchases",
        uniqueConstraints ={
        @UniqueConstraint(columnNames = {"buyer_id", "data_post_id"})
        })
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private AppUser buyer;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private AppUser seller;

    @ManyToOne
    @JoinColumn(name = "data_post_id", nullable = false)
    private DataPost post;

    @Enumerated(EnumType.STRING)
    private Genre genre;

    private BigDecimal pricePaid;
    private String titleSnapshot;
    private String summarySnapshot;
    private String contentSnapshot;
    private Instant purchasedAt;

    public Purchase() {}

    public Purchase(
            AppUser buyer,
            AppUser seller,
            DataPost post,
            BigDecimal pricePaid,
            String titleSnapshot,
            String summarySnapshot,
            String contentSnapshot,
            Instant purchasedAt
    ) {
        this.buyer = buyer;
        this.seller = seller;
        this.post = post;
        this.genre = post.getGenre();
        this.pricePaid = pricePaid;
        this.titleSnapshot = titleSnapshot;
        this.summarySnapshot = summarySnapshot;
        this.contentSnapshot = contentSnapshot;
        this.purchasedAt = purchasedAt;
    }

    public Long getId() {
        return this.id;
    }

    public AppUser getBuyer() {
        return this.buyer;
    }

    public AppUser getSeller() {
        return this.seller;
    }

    public Genre getGenre() {
        return this.genre;
    }

    public DataPost getDataPost() {
        return this.post;
    }

    public BigDecimal getPricePaid() {
        return this.pricePaid;
    }

    public String getTitleSnapshot() {
        return this.titleSnapshot;
    }

    public String getSummarySnapshot() {
        return this.summarySnapshot;
    }

    public String getContentSnapshot() {
        return this.contentSnapshot;
    }

    public Instant getPurchasedAt() {
        return this.purchasedAt;
    }
}
