package com.aihealthcoach.common.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aihealthcoach.user.exception.UserException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET_KEY =
            "0123456789012345678901234567890123456789012345678901234567890123";
    private static final long USER_ID = 1L;

    @Test
    void createAccessTokenContainsUserIdAndTokenId() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET_KEY, 3_600_000L, 1_209_600_000L);

        String accessToken = tokenProvider.createAccessToken(USER_ID);

        assertThat(tokenProvider.getUserId(accessToken)).isEqualTo(USER_ID);
        assertThat(tokenProvider.getTokenId(accessToken)).isNotBlank();
        assertThat(tokenProvider.getRemaining(accessToken)).isPositive();
    }

    @Test
    void validateAccessTokenRejectsRefreshToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET_KEY, 3_600_000L, 1_209_600_000L);
        String refreshToken = tokenProvider.createRefreshToken(USER_ID);

        assertThatThrownBy(() -> tokenProvider.validateAccessToken(refreshToken))
                .isInstanceOf(UserException.class);
    }

    @Test
    void validateRefreshTokenRejectsAccessToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET_KEY, 3_600_000L, 1_209_600_000L);
        String accessToken = tokenProvider.createAccessToken(USER_ID);

        assertThatThrownBy(() -> tokenProvider.validateRefreshToken(accessToken))
                .isInstanceOf(UserException.class);
    }

    @Test
    void expiredAccessTokenIsRejected() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET_KEY, -1L, 1_209_600_000L);
        String expiredAccessToken = tokenProvider.createAccessToken(USER_ID);

        assertThatThrownBy(() -> tokenProvider.validateAccessToken(expiredAccessToken))
                .isInstanceOf(UserException.class);
        assertThatThrownBy(() -> tokenProvider.getRemaining(expiredAccessToken))
                .isInstanceOf(UserException.class);
    }

    @Test
    void remainingTtlIsBasedOnTokenExpiration() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET_KEY, 3_600_000L, 1_209_600_000L);
        String accessToken = tokenProvider.createAccessToken(USER_ID);

        Duration remaining = tokenProvider.getRemaining(accessToken);

        assertThat(remaining).isBetween(Duration.ofMinutes(59), Duration.ofMinutes(60));
    }
}
