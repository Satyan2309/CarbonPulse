package com.ecotrack.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // Access Token: 15 minutes
    public static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000L;

    // Refresh Token: 7 days
    public static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    /**
     * Returns the HMAC-SHA256 signing key.
     * The secret must be a Base64-encoded 256-bit key supplied via
     * JWT_SECRET env var or application-local.properties.
     */
    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            // Fallback for plain-text secrets during local dev if Base64 decoding fails
            return Keys.hmacShaKeyFor(secret.getBytes());
        }
    }

    // -------------------------------------------------------
    // Token Generation
    // -------------------------------------------------------

    public String generateAccessToken(String email) {
        return generateToken(email, ACCESS_TOKEN_EXPIRATION, "ACCESS", null);
    }

    /**
     * Generates an access token with role and userId embedded as claims.
     * This eliminates the need for a DB lookup in the JWT filter on every request.
     */
    public String generateAccessToken(String email, String role, Long userId) {
        return generateToken(email, ACCESS_TOKEN_EXPIRATION, "ACCESS", Map.of("role", role, "uid", userId));
    }

    public String generateRefreshToken(String email) {
        return generateToken(email, REFRESH_TOKEN_EXPIRATION, "REFRESH", null);
    }

    public String generateRefreshToken(String email, String role, Long userId) {
        return generateToken(email, REFRESH_TOKEN_EXPIRATION, "REFRESH", Map.of("role", role, "uid", userId));
    }

    /** @deprecated Use {@link #generateAccessToken(String)} instead. */
    @Deprecated
    public String generateToken(String email) {
        return generateAccessToken(email);
    }

    public String generateToken(String email, long expirationMs, String tokenType) {
        return generateToken(email, expirationMs, tokenType, null);
    }

    public String generateToken(String email, long expirationMs, String tokenType, Map<String, Object> extraClaims) {
        var builder = Jwts.builder()
                .subject(email)
                .claim("type", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs));

        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }

        return builder.signWith(getSigningKey()).compact();
    }

    // -------------------------------------------------------
    // Token Extraction
    // -------------------------------------------------------

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extracts the role from the JWT claims.
     * Returns "USER" if no role claim is present (backward compat with old tokens).
     */
    public String extractRole(String token) {
        Claims claims = getClaims(token);
        String role = claims.get("role", String.class);
        return role != null ? role : "USER";
    }

    /**
     * Extracts the userId from the JWT claims.
     * Returns null if no uid claim is present (backward compat with old tokens).
     */
    public Long extractUserId(String token) {
        Claims claims = getClaims(token);
        Object uid = claims.get("uid");
        if (uid == null) return null;
        if (uid instanceof Long l) return l;
        if (uid instanceof Integer i) return i.longValue();
        return null;
    }

    // -------------------------------------------------------
    // Token Validation
    // -------------------------------------------------------

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaims(token);
            String type = claims.get("type", String.class);
            return type == null || "REFRESH".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
