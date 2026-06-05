package com.aihealthcoach.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.user.dto.UserDto.LoginRequest;
import com.aihealthcoach.user.dto.UserDto.LoginResponse;
import com.aihealthcoach.user.dto.UserDto.LoginResult;
import com.aihealthcoach.user.dto.UserDto.LogoutResponse;
import com.aihealthcoach.user.dto.UserDto.SignupRequest;
import com.aihealthcoach.user.dto.UserDto.TokenRefreshResponse;
import com.aihealthcoach.user.dto.UserDto.UserProfileResponse;
import com.aihealthcoach.user.dto.UserDto.UserProfileUpdateRequest;
import com.aihealthcoach.user.exception.UserException;
import com.aihealthcoach.user.service.UserService;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@RestController
@RequestMapping("api/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    @Value("${security.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${security.cookie.secure}")
    private boolean cookieSecure;

    @Value("${security.cookie.same-site}")
    private String cookieSameSite;

    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(@Valid @RequestBody SignupRequest request){
        return ResponseEntity.ok(userService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request){
        LoginResult loginResult = userService.login(request);
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", loginResult.refreshToken())
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path("/api/user")
            .maxAge(refreshTokenExpirationMs / 1000)
            .build();
        
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(loginResult.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
        @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @Parameter(hidden = true) @CookieValue("refreshToken") String refreshToken) {

        String accessToken = extractBearerToken(authorization);

        userService.logout(accessToken, refreshToken);

        ResponseCookie expiredRefreshCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path("/api/user")
            .maxAge(0)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie.toString())
            .body(LogoutResponse.builder()
                .message("정상적으로 로그아웃되었습니다.")
                .build());
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshAccessToken(
            @Parameter(hidden = true) @CookieValue("refreshToken") String refreshToken) {
        return ResponseEntity.ok(userService.refreshAccessToken(refreshToken));
    }
    
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> findProfile(
        Authentication authentication){
        
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userService.findProfile(userId));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
        @RequestBody UserProfileUpdateRequest request,
        Authentication authentication){
        
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }
    
    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw UserException.invalidToken();
        }

        return authorization.substring("Bearer ".length());
    }
}
