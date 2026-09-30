package com.campusbite.auth;

import com.campusbite.common.BaseEntity;
import com.campusbite.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Instant;
import com.campusbite.common.util.TokenHashUtil;

/**
 * Opaque, DB-backed refresh token. Tokens are rotated on every use: each
 * refresh call revokes the presented token and issues a brand new one,
 * chaining via replacedByToken so token-reuse (theft) can be detected.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Transient
    private String rawToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "replaced_by_token_hash", length = 64)
    private String replacedByTokenHash;

    @Column
    private String userAgent;

    public RefreshToken() {
    }

    public RefreshToken(String token, User user, Instant expiresAt, String userAgent) {
        this.rawToken = token;
        this.tokenHash = TokenHashUtil.sha256(token);
        this.user = user;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }

    /** Raw token is available only on the newly-created in-memory entity. Loaded entities never contain it. */
    public String getToken() {
        return rawToken;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public User getUser() {
        return user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public String getReplacedByTokenHash() {
        return replacedByTokenHash;
    }

    public void setReplacedByTokenHash(String replacedByTokenHash) {
        this.replacedByTokenHash = replacedByTokenHash;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
