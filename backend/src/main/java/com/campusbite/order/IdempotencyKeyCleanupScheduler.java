package com.campusbite.order;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Keeps the idempotency_keys table from growing forever -- expired rows are useless. */
@Component
public class IdempotencyKeyCleanupScheduler {

    private final IdempotencyKeyRepository repository;

    public IdempotencyKeyCleanupScheduler(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 3 * * ?") // 3 AM daily
    @Transactional
    public void purgeExpiredKeys() {
        List<IdempotencyKeyRecord> expired = repository.findByExpiresAtBefore(Instant.now());
        repository.deleteAll(expired);
    }
}