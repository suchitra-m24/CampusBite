package com.campusbite.order;

import com.campusbite.common.exception.ConflictException;
import com.campusbite.common.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
public class IdempotencyService {

    private static final int KEY_LIFETIME_HOURS = 24;

    private final IdempotencyKeyRepository repository;
    private final TransactionTemplate requiresNewTransaction;

    public IdempotencyService(IdempotencyKeyRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public String hashRequest(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                digest.update((part == null ? "" : part).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0); // separator, avoids "ab"+"c" colliding with "a"+"bc"
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Each branch runs in its OWN independent transaction via
     * TransactionTemplate (PROPAGATION_REQUIRES_NEW) rather than a shared
     * @Transactional method. This matters specifically because the "insert
     * and see if it collides" attempt can throw a
     * DataIntegrityViolationException -- Hibernate poisons the current
     * session the moment any exception occurs mid-flush, so reusing that
     * same session for the fallback lookup would itself blow up with
     * "don't flush the Session after an exception occurs". Running the
     * insert attempt and the fallback lookup as two separate transactions
     * keeps them on two separate, healthy sessions.
     */
    public ClaimOutcome claim(String idempotencyKey, Long userId, String requestHash) {
        try {
            Long recordId = requiresNewTransaction.execute(status -> {
                IdempotencyKeyRecord record = repository.saveAndFlush(new IdempotencyKeyRecord(
                        idempotencyKey, userId, requestHash,
                        Instant.now().plus(KEY_LIFETIME_HOURS, ChronoUnit.HOURS)));
                return record.getId();
            });
            return new ClaimOutcome(recordId, false, null);
        } catch (DataIntegrityViolationException e) {
            return resolveAgainstExistingClaim(idempotencyKey, userId, requestHash);
        }
    }

    private ClaimOutcome resolveAgainstExistingClaim(String idempotencyKey, Long userId, String requestHash) {
        ExistingClaimCheck check = requiresNewTransaction.execute(status -> {
            IdempotencyKeyRecord existing = repository.findByIdempotencyKeyAndUserId(idempotencyKey, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Idempotency key claim vanished unexpectedly"));

            if (existing.isExpired()) {
                repository.delete(existing);
                return new ExistingClaimCheck(true, null, null, null);
            }
            return new ExistingClaimCheck(false, existing.getRequestHash(), existing.getId(), existing.getOrderId());
        });

        if (check.wasExpiredAndDeleted()) {
            // Stale key from a previous cycle -- now that it's cleaned up in its own
            // committed transaction, treat this as a genuinely fresh claim attempt.
            return claim(idempotencyKey, userId, requestHash);
        }
        if (!check.storedRequestHash().equals(requestHash)) {
            throw new ConflictException("This idempotency key was already used with a different request");
        }
        if (check.orderId() == null) {
            throw new ConflictException("This request is already being processed. Please wait and retry.");
        }
        return new ClaimOutcome(check.recordId(), true, check.orderId());
    }

    public void recordResult(Long recordId, Long orderId) {
        requiresNewTransaction.executeWithoutResult(status -> {
            IdempotencyKeyRecord record = repository.findById(recordId)
                    .orElseThrow(() -> new ResourceNotFoundException("Idempotency key record not found"));
            record.setOrderId(orderId);
        });
    }

    /** Releases a pending claim when the actual order transaction fails. */
    public void release(Long recordId) {
        requiresNewTransaction.executeWithoutResult(status -> repository.deleteById(recordId));
    }

    public record ClaimOutcome(Long recordId, boolean alreadyCompleted, Long existingOrderId) {
    }

    private record ExistingClaimCheck(boolean wasExpiredAndDeleted, String storedRequestHash, Long recordId, Long orderId) {
    }
}