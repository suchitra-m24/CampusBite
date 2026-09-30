package com.campusbite.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Prevents authentication-token tables from growing without bound. */
@Component
public class AuthTokenCleanupScheduler {
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final EmailVerificationTokenRepository verificationTokens;

    public AuthTokenCleanupScheduler(RefreshTokenRepository refreshTokens,
                                     PasswordResetTokenRepository resetTokens,
                                     EmailVerificationTokenRepository verificationTokens) {
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.verificationTokens = verificationTokens;
    }

    @Scheduled(cron = "0 20 3 * * ?")
    @Transactional
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        refreshTokens.deleteByRevokedTrueOrExpiresAtBefore(now.minus(7, ChronoUnit.DAYS));
        resetTokens.deleteByUsedTrueOrExpiresAtBefore(now);
        verificationTokens.deleteByUsedTrueOrExpiresAtBefore(now);
    }
}
