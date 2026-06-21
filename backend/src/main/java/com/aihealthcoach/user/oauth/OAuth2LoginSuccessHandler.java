package com.aihealthcoach.user.oauth;

import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.user.dto.OAuthUserDto;
import com.aihealthcoach.user.entity.User;
import com.aihealthcoach.user.service.OAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthService oauthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRedisRepository tokenRedisRepository;

    @Value("${security.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${security.cookie.secure}")
    private boolean cookieSecure;

    @Value("${security.cookie.same-site}")
    private String cookieSameSite;

    @Value("${app.oauth.success-redirect-url}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oauth2User = oauthToken.getPrincipal();

        OAuthUserDto userDto = OAuthUserDtoFactory.from(registrationId, oauth2User);
        User user = oauthService.findOrCreateOAuthUser(userDto);

        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        String refreshTokenId = jwtTokenProvider.getTokenId(refreshToken);

        tokenRedisRepository.saveRefreshToken(
                user.getId(),
                refreshTokenId,
                refreshToken,
                jwtTokenProvider.getRemaining(refreshToken)
        );

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/user")
                .maxAge(refreshTokenExpirationMs / 1000)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.sendRedirect(successRedirectUrl);
    }
}