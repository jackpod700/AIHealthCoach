package com.aihealthcoach.common.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.aihealthcoach.user.exception.UserException;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {
    
    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    public JwtTokenProvider(
        @Value("${security.jwt.token.secret-key}") String secretKey,
        @Value("${security.jwt.access-token-expiration-ms}") long accessTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    } 

    public String createAccessToken(Long userId){
        final Date now = new Date();
        final Date expiration = new Date(now.getTime() + accessTokenExpirationMs);
        
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Long getUserId(String token) {
        try{
            String id = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject();

            return Long.valueOf(id);
        } catch (JwtException | IllegalArgumentException exception) {
            throw UserException.invalidToken();
        }
    }
}
