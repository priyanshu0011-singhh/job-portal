package com.ncst.job.portal.security;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenHelper {

    private static final long MIN_SECRET_BYTES = 64;

    private static final String DEFAULT_LOCAL_SECRET = "dev-local-secret-key-change-me-please-change";

    private final Environment environment;

    public JwtTokenHelper(Environment environment) {
        this.environment = environment;
    }

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs:86400000}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        String resolvedSecret = jwtSecret == null || jwtSecret.isBlank() ? DEFAULT_LOCAL_SECRET : jwtSecret;

        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            if (resolvedSecret.isBlank()) {
                throw new IllegalStateException("JWT_SECRET must be set in production");
            }
            try {
                byte[] keyBytes = Decoders.BASE64.decode(resolvedSecret);
                if (keyBytes.length < MIN_SECRET_BYTES) {
                    throw new IllegalStateException("JWT_SECRET must be valid Base64 encoding of at least 64 random bytes");
                }
                return Keys.hmacShaKeyFor(keyBytes);
            } catch (RuntimeException e) {
                throw new IllegalStateException("JWT_SECRET must be valid Base64 encoding of at least 64 random bytes", e);
            }
        }

        if (environment.acceptsProfiles(Profiles.of("render"))) {
            if (resolvedSecret.isBlank()) {
                throw new IllegalStateException("JWT_SECRET must be set in the Render profile");
            }
            byte[] secretBytes;
            try {
                byte[] decodedBytes = Decoders.BASE64.decode(resolvedSecret);
                secretBytes = decodedBytes.length >= 32
                        ? decodedBytes
                        : resolvedSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            } catch (RuntimeException e) {
                secretBytes = resolvedSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            if (secretBytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET must contain at least 32 random bytes in the Render profile");
            }
            try {
                return Keys.hmacShaKeyFor(MessageDigest.getInstance("SHA-512").digest(secretBytes));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-512 is unavailable for JWT key derivation", e);
            }
        }

        try {
            byte[] keyBytes = Decoders.BASE64.decode(resolvedSecret);
            if (keyBytes.length >= MIN_SECRET_BYTES) {
                return Keys.hmacShaKeyFor(keyBytes);
            }
        } catch (RuntimeException ignored) {
            // Fall through to a safe local-development key derived from the configured secret.
        }

        byte[] fallbackBytes = resolvedSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (fallbackBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret must be at least 64 bytes for local development");
        }
        return Keys.hmacShaKeyFor(java.util.Arrays.copyOf(fallbackBytes, 64));
    }


    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(userDetails.getUsername())   // must match loadUserByUsername param
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validate(String token, UserDetails userDetails) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey()).build()
                    .parseClaimsJws(token).getBody();
            return claims.getSubject().equals(userDetails.getUsername()) && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}




