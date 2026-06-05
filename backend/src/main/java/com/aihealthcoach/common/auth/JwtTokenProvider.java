package com.aihealthcoach.common.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.aihealthcoach.user.exception.UserException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {
    
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
        @Value("${security.jwt.token.secret-key}") String secretKey,
        @Value("${security.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
        @Value("${security.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    } 

    public String createAccessToken(Long userId){
        return createToken(userId, ACCESS_TOKEN_TYPE, accessTokenExpirationMs);
    }

    public String createRefreshToken(Long userId){
        return createToken(userId, REFRESH_TOKEN_TYPE, refreshTokenExpirationMs);
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getTokenId(String token){
        return parseClaims(token).getId();
    }

    public Duration getRemaining(String token){
        Date expiration = parseClaims(token).getExpiration();
        long remainingMs = expiration.getTime() - System.currentTimeMillis();

        if (remainingMs <= 0) {
            throw UserException.invalidToken();
        }

        return Duration.ofMillis(remainingMs);
    }

    public void validateAccessToken(String token) {
        validateTokenType(token, ACCESS_TOKEN_TYPE);
    }

    public void validateRefreshToken(String token) {
        validateTokenType(token, REFRESH_TOKEN_TYPE);
    }

    private String createToken(Long userId, String tokenType, long expirationMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    private void validateTokenType(String token, String expectedType) {
        String tokenType = parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class);

        if (!expectedType.equals(tokenType)) {
            throw UserException.invalidToken();
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException exception) {
            throw UserException.invalidToken();
        }
    }
}
