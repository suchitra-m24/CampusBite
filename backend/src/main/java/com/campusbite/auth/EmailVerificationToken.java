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

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Transient
    private String rawToken;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    public EmailVerificationToken() {
    }

    public EmailVerificationToken(User user, String token, Instant expiresAt) {
        this.user = user;
        this.rawToken = token;
        this.tokenHash = TokenHashUtil.sha256(token);
        this.expiresAt = expiresAt;
    }

    public boolean isValid() {
        return !used && Instant.now().isBefore(expiresAt);
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return rawToken;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
