package com.aihealthcoach.common.auth;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TokenRedisRepository {
    private static final  String REFRESH_TOKEN_PREFIX = "refresh";
    private static final String ACCESS_TOKEN_BLACKLIST_PREFIX = "blacklist:access";

    private final StringRedisTemplate redisTemplate;

    public void saveRefreshToken(Long userId, String refreshTokenId, String refreshToken, Duration ttl) {
        
        
        redisTemplate.opsForValue()
            .set(refreshTokenKey(userId, refreshTokenId), refreshToken, ttl);

    }

    public Optional<String> findRefreshToken(Long userId, String refreshTokenId) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(refreshTokenKey(userId, refreshTokenId))
        );
    }

    public void deleteRefreshToken(Long userId, String refreshTokenId) {
        redisTemplate.delete(refreshTokenKey(userId, refreshTokenId));
    }

    public void blacklistAccessToken(String accessTokenId, Duration ttl) {
        redisTemplate.opsForValue()
            .set(accessTokenBlacklistKey(accessTokenId), "logout", ttl);
    }

    public boolean isAccessTokenBlacklisted(String accessTokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(accessTokenBlacklistKey(accessTokenId)));
    }

    private String refreshTokenKey(Long userId, String refreshTokenId) {
        return REFRESH_TOKEN_PREFIX + ":" + userId + ":" + refreshTokenId;
    }

    private String accessTokenBlacklistKey(String accessTokenId) {
        return ACCESS_TOKEN_BLACKLIST_PREFIX + ":" + accessTokenId;
    }
}
