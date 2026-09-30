package com.campusbite.order;

import com.campusbite.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * One row per client-supplied Idempotency-Key. Claiming a key is an atomic
 * INSERT guarded by the unique constraint below -- if two requests with
 * the same key race, exactly one INSERT succeeds; the loser catches the
 * constraint violation and looks up what the winner is doing/did instead
 * of proceeding to create a duplicate order.
 */
@Entity
@Table(name = "idempotency_keys", uniqueConstraints =
@UniqueConstraint(columnNames = {"idempotency_key", "user_id"}))
public class IdempotencyKeyRecord extends BaseEntity {

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String requestHash;

    /** Null while the original request is still being processed; set once the order is created. */
    @Column
    private Long orderId;

    @Column(nullable = false)
    private Instant expiresAt;

    public IdempotencyKeyRecord() {
    }

    public IdempotencyKeyRecord(String idempotencyKey, Long userId, String requestHash, Instant expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.requestHash = requestHash;
        this.expiresAt = expiresAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
} 