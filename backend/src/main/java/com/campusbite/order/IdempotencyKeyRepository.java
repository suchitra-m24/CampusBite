package com.campusbite.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyRecord, Long> {
    Optional<IdempotencyKeyRecord> findByIdempotencyKeyAndUserId(String idempotencyKey, Long userId);
    List<IdempotencyKeyRecord> findByExpiresAtBefore(Instant cutoff);
}