package com.campus.campus_backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration-minutes}")
    private long expirationMinutes;

    public String generate(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(expirationMinutes * 60_000);
        return Jwts.builder()
                .header().add("typ", "JWT").and()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public long getExpirationMillis() {
        return expirationMinutes * 60_000;
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSubject(String token) {
        return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public long getExpirationEpochMillis(String token) {
        return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).build()
                .parseSignedClaims(token).getPayload().getExpiration().getTime();
    }
}
