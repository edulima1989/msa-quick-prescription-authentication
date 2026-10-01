package com.quickprescription.authentication.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final String DEFAULT_JWT_SECRET =
            "replace-this-64-byte-minimum-jwt-secret-before-production-use-2026-auth";

    @Value("${jwt.secret:" + DEFAULT_JWT_SECRET + "}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationMs;

    private int expirationSeconds;

    @PostConstruct
    void initExpiration() {
        long seconds = jwtExpirationMs / 1000;
        if (seconds < 1 || seconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("jwt.expiration debe estar entre 1000 ms y " + Integer.MAX_VALUE + " s");
        }
        expirationSeconds = (int) seconds;
    }

    /** Vigencia del token en segundos (jwt.expiration / 1000); es el expiresIn del login. */
    public int getExpirationSeconds() {
        return expirationSeconds;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String userMail, String userRole) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userRole", userRole);
        return createToken(claims, userMail);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        // iat y exp se guardan en segundos; se trunca para que exp - iat sea exactamente expiresIn.
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiry = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        String userRole = claims.get("userRole", String.class);

        return new UsernamePasswordAuthenticationToken(
                claims.getSubject(),
                token,
                userRole == null || userRole.isBlank()
                        ? List.of()
                        : List.of(new SimpleGrantedAuthority("ROLE_" + userRole))
        );
    }

    public String extractUserMail(String token) {
        return getClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    public String extractUserRole(String token) {
        return getClaims(token).get("userRole", String.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
