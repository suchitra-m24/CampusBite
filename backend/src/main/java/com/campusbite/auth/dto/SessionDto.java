package com.campusbite.auth.dto;

import com.campusbite.auth.RefreshToken;

import java.time.Instant;

public record SessionDto(
        Long id,
        String userAgent,
        Instant createdAt,
        Instant expiresAt,
        boolean current
) {
    public static SessionDto from(RefreshToken t, boolean current) {
        return new SessionDto(t.getId(), t.getUserAgent(), t.getCreatedAt(), t.getExpiresAt(), current);
    }
}
