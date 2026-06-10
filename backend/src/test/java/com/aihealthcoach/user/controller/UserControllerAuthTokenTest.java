package com.aihealthcoach.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aihealthcoach.user.dto.UserDto.LoginRequest;
import com.aihealthcoach.user.dto.UserDto.LoginResponse;
import com.aihealthcoach.user.dto.UserDto.LoginResult;
import com.aihealthcoach.user.dto.UserDto.LogoutResponse;
import com.aihealthcoach.user.dto.UserDto.TokenRefreshResponse;
import com.aihealthcoach.user.exception.UserException;
import com.aihealthcoach.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserControllerAuthTokenTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    @Mock
    private UserService userService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService);
        ReflectionTestUtils.setField(userController, "refreshTokenExpirationMs", 1_209_600_000L);
        ReflectionTestUtils.setField(userController, "cookieSecure", false);
        ReflectionTestUtils.setField(userController, "cookieSameSite", "Lax");
    }

    @Test
    void loginSetsRefreshTokenCookieAndReturnsAccessTokenBody() {
        LoginRequest request = LoginRequest.builder().email("test@example.com").password("password").build();
        LoginResponse response = LoginResponse.builder()
                .userId(1L)
                .email("test@example.com")
                .nickname("테스트유저")
                .accessToken(ACCESS_TOKEN)
                .build();
        when(userService.login(request)).thenReturn(LoginResult.builder()
                .response(response)
                .refreshToken(REFRESH_TOKEN)
                .build());

        ResponseEntity<LoginResponse> result = userController.login(request);

        String setCookie = result.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(result.getBody()).isEqualTo(response);
        assertThat(setCookie).contains("refreshToken=" + REFRESH_TOKEN);
        assertThat(setCookie).contains("Path=/api/user");
        assertThat(setCookie).contains("Max-Age=1209600");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Lax");
    }

    @Test
    void refreshAccessTokenUsesRefreshCookie() {
        TokenRefreshResponse response = TokenRefreshResponse.builder().accessToken("new-access-token").build();
        when(userService.refreshAccessToken(REFRESH_TOKEN)).thenReturn(response);

        ResponseEntity<TokenRefreshResponse> result = userController.refreshAccessToken(REFRESH_TOKEN);

        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void logoutPassesAccessTokenAndRefreshTokenThenExpiresCookie() {
        ResponseEntity<LogoutResponse> result = userController.logout("Bearer " + ACCESS_TOKEN, REFRESH_TOKEN);

        verify(userService).logout(ACCESS_TOKEN, REFRESH_TOKEN);
        String setCookie = result.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(LogoutResponse.builder().build());
        assertThat(result.getBody().message()).isEqualTo("정상적으로 로그아웃되었습니다.");
        assertThat(setCookie).contains("refreshToken=");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("HttpOnly");
    }

    @Test
    void logoutRejectsAuthorizationHeaderWithoutBearerPrefix() {
        assertThatThrownBy(() -> userController.logout("Basic " + ACCESS_TOKEN, REFRESH_TOKEN))
                .isInstanceOf(UserException.class);
    }
}
