package com.campusbite.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.Instant;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    long deleteByUsedTrueOrExpiresAtBefore(Instant cutoff);

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from EmailVerificationToken t where t.tokenHash = :tokenHash")
    Optional<EmailVerificationToken> findByTokenForUpdate(@Param("tokenHash") String tokenHash);
}
