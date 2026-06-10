package com.aihealthcoach.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.user.dto.UserDto.LoginRequest;
import com.aihealthcoach.user.dto.UserDto.LoginResult;
import com.aihealthcoach.user.dto.UserDto.TokenRefreshResponse;
import com.aihealthcoach.user.entity.User;
import com.aihealthcoach.user.exception.UserException;
import com.aihealthcoach.user.mapper.UserMapper;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplAuthTokenTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String ACCESS_TOKEN_ID = "access-token-id";
    private static final String REFRESH_TOKEN_ID = "refresh-token-id";
    private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_TTL = Duration.ofDays(14);

    @Mock
    private UserMapper userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenRedisRepository tokenRedisRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void loginCreatesRefreshTokenAndStoresItInRedis() {
        User user = user();
        LoginRequest request = LoginRequest.builder().email(EMAIL).password(PASSWORD).build();
        when(userDao.findUserByEmail(EMAIL)).thenReturn(user);
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(USER_ID)).thenReturn(ACCESS_TOKEN);
        when(jwtTokenProvider.createRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);
        when(jwtTokenProvider.getTokenId(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_ID);
        when(jwtTokenProvider.getRemaining(REFRESH_TOKEN)).thenReturn(REFRESH_TTL);

        LoginResult result = userService.login(request);

        assertThat(result.response().accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
        verify(tokenRedisRepository).saveRefreshToken(USER_ID, REFRESH_TOKEN_ID, REFRESH_TOKEN, REFRESH_TTL);
    }

    @Test
    void loginRejectsUnknownEmail() {
        LoginRequest request = LoginRequest.builder().email(EMAIL).password(PASSWORD).build();
        when(userDao.findUserByEmail(EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UserException.class);
    }

    @Test
    void loginRejectsInvalidPassword() {
        LoginRequest request = LoginRequest.builder().email(EMAIL).password(PASSWORD).build();
        when(userDao.findUserByEmail(EMAIL)).thenReturn(user());
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UserException.class);
    }

    @Test
    void refreshAccessTokenIssuesNewAccessToken() {
        when(jwtTokenProvider.getUserId(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.getTokenId(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_ID);
        when(tokenRedisRepository.findRefreshToken(USER_ID, REFRESH_TOKEN_ID)).thenReturn(Optional.of(REFRESH_TOKEN));
        when(jwtTokenProvider.createAccessToken(USER_ID)).thenReturn(NEW_ACCESS_TOKEN);

        TokenRefreshResponse response = userService.refreshAccessToken(REFRESH_TOKEN);

        verify(jwtTokenProvider).validateRefreshToken(REFRESH_TOKEN);
        assertThat(response.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
    }

    @Test
    void refreshAccessTokenRejectsMissingRedisToken() {
        when(jwtTokenProvider.getUserId(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.getTokenId(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_ID);
        when(tokenRedisRepository.findRefreshToken(USER_ID, REFRESH_TOKEN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.refreshAccessToken(REFRESH_TOKEN))
                .isInstanceOf(UserException.class);
    }

    @Test
    void refreshAccessTokenRejectsMismatchedRedisToken() {
        when(jwtTokenProvider.getUserId(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.getTokenId(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_ID);
        when(tokenRedisRepository.findRefreshToken(USER_ID, REFRESH_TOKEN_ID)).thenReturn(Optional.of("other-token"));

        assertThatThrownBy(() -> userService.refreshAccessToken(REFRESH_TOKEN))
                .isInstanceOf(UserException.class);
    }

    @Test
    void refreshAccessTokenRejectsAccessTokenUsedAsRefreshToken() {
        doThrow(UserException.invalidToken()).when(jwtTokenProvider).validateRefreshToken(ACCESS_TOKEN);

        assertThatThrownBy(() -> userService.refreshAccessToken(ACCESS_TOKEN))
                .isInstanceOf(UserException.class);

        verify(tokenRedisRepository, never()).findRefreshToken(USER_ID, REFRESH_TOKEN_ID);
    }

    @Test
    void logoutBlacklistsAccessTokenAndDeletesRefreshToken() {
        when(jwtTokenProvider.getUserId(ACCESS_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.getUserId(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.getTokenId(ACCESS_TOKEN)).thenReturn(ACCESS_TOKEN_ID);
        when(jwtTokenProvider.getTokenId(REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_ID);
        when(jwtTokenProvider.getRemaining(ACCESS_TOKEN)).thenReturn(ACCESS_TTL);

        userService.logout(ACCESS_TOKEN, REFRESH_TOKEN);

        verify(jwtTokenProvider).validateAccessToken(ACCESS_TOKEN);
        verify(jwtTokenProvider).validateRefreshToken(REFRESH_TOKEN);
        verify(tokenRedisRepository).blacklistAccessToken(ACCESS_TOKEN_ID, ACCESS_TTL);
        verify(tokenRedisRepository).deleteRefreshToken(USER_ID, REFRESH_TOKEN_ID);
    }

    @Test
    void logoutRejectsAccessAndRefreshTokenUserMismatch() {
        when(jwtTokenProvider.getUserId(ACCESS_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.getUserId(REFRESH_TOKEN)).thenReturn(OTHER_USER_ID);

        assertThatThrownBy(() -> userService.logout(ACCESS_TOKEN, REFRESH_TOKEN))
                .isInstanceOf(UserException.class);

        verify(tokenRedisRepository, never()).blacklistAccessToken(ACCESS_TOKEN_ID, ACCESS_TTL);
        verify(tokenRedisRepository, never()).deleteRefreshToken(OTHER_USER_ID, REFRESH_TOKEN_ID);
    }

    @Test
    void logoutRejectsExpiredAccessToken() {
        doThrow(UserException.invalidToken()).when(jwtTokenProvider).validateAccessToken(ACCESS_TOKEN);

        assertThatThrownBy(() -> userService.logout(ACCESS_TOKEN, REFRESH_TOKEN))
                .isInstanceOf(UserException.class);

        verify(tokenRedisRepository, never()).blacklistAccessToken(ACCESS_TOKEN_ID, ACCESS_TTL);
        verify(tokenRedisRepository, never()).deleteRefreshToken(USER_ID, REFRESH_TOKEN_ID);
    }

    private User user() {
        return User.builder()
                .id(USER_ID)
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .nickname("테스트유저")
                .build();
    }
}
