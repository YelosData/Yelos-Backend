package com.yelos.backend.user.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String username;
    private String email;
    private String passwordHash;
    private BigDecimal credits;

    @Enumerated(EnumType.STRING)
    private Role role;
    private Instant createdAt;
    private Instant updatedAt;

    public AppUser() {}

    public AppUser(
            String userName,
            String email,
            String passwordHash,
            BigDecimal credits,
            Role role,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.username = userName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.credits = credits;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public BigDecimal getCredits() {
        return this.credits;
    }

    public Role getRole() {
        return this.role;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public void deductCred(BigDecimal credits) {
        if (this.credits.compareTo(credits) < 0) {
            throw new RuntimeException("Insufficient credits");
        }
        this.credits = this.credits.subtract(credits);
    }

    public void addCred(BigDecimal credits) {
        this.credits = this.credits.add(credits);
    }
}
