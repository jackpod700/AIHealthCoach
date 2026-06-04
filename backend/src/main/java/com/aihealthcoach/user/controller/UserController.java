package com.aihealthcoach.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.user.dto.UserDto.LoginRequest;
import com.aihealthcoach.user.dto.UserDto.LoginResponse;
import com.aihealthcoach.user.dto.UserDto.LoginResult;
import com.aihealthcoach.user.dto.UserDto.SignupRequest;
import com.aihealthcoach.user.dto.UserDto.UserProfileResponse;
import com.aihealthcoach.user.dto.UserDto.UserProfileUpdateRequest;
import com.aihealthcoach.user.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


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
    public ResponseEntity<LoginResponse> signup(@RequestBody SignupRequest request){
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
    
}
