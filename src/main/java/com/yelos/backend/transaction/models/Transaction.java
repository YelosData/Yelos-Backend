package com.yelos.backend.transaction.models;

import com.yelos.backend.purchase.models.Purchase;
import com.yelos.backend.user.models.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private Instant createdAt;

    public Transaction() {}
    public Transaction(
            AppUser user,
            Purchase purchase,
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            Instant createdAt
    ) {
        this.user = user;
        this.purchase = purchase;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return this.id;
    }

    public AppUser getUser() {
        return this.user;
    }

    public Purchase getPurchase() {
        return this.purchase;
    }

    public TransactionType getTransactionType() {
        return this.type;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public BigDecimal getBalanceAfter() {
        return this.balanceAfter;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
