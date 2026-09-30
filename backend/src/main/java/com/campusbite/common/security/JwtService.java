package com.campusbite.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Issues and validates short-lived JWT ACCESS tokens.
 * Refresh tokens are opaque, DB-backed, and rotated on every use
 * (see auth.RefreshToken) -- the JWT layer here only covers access tokens.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenExpiryMs;

    public JwtService(
            @Value("${campusbite.jwt.secret}") String secret,
            @Value("${campusbite.jwt.access-token-expiry-ms}") long accessTokenExpiryMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
    }

    public String generateAccessToken(Long userId, String loginId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiryMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of("loginId", loginId, "role", role))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String extractLoginId(String token) {
        return extractAllClaims(token).get("loginId", String.class);
    }

    public Long extractUserId(String token) {
        return Long.valueOf(extractClaim(token, Claims::getSubject));
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
