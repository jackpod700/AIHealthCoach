package com.aihealthcoach.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.user.dto.LoginRequest;
import com.aihealthcoach.user.dto.LoginResponse;
import com.aihealthcoach.user.dto.SignupRequest;
import com.aihealthcoach.user.dto.UserProfileResponse;
import com.aihealthcoach.user.dto.UserProfileUpdateRequest;
import com.aihealthcoach.user.exception.UserException;
import com.aihealthcoach.user.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("api/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(@RequestBody SignupRequest request){
        return ResponseEntity.ok(userService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request){
        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> findProfile(
        @PathVariable Long userId,
        Authentication authentication){
        
        Long loginUserId = (Long) authentication.getPrincipal();

        if (!loginUserId.equals(userId)){
            throw UserException.profileAccessDenied();
        }

        return ResponseEntity.ok(userService.findProfile(userId));
    }

    @PatchMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
        @PathVariable Long userId, 
        @RequestBody UserProfileUpdateRequest request,
        Authentication authentication){
        
        Long loginUserId = (Long) authentication.getPrincipal();

        if (!loginUserId.equals(userId)) {
            throw UserException.profileAccessDenied();
        }

        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }
    
}
