package com.aihealthcoach.user.controller;

import com.aihealthcoach.user.exception.UserException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/oauth")
public class OAuthController {

    private static final Set<String> SUPPORT_PROVIDERS = Set.of("google", "naver");

    @GetMapping("/login/{provider}")
    public ResponseEntity<Void> OAuthLogin(
            @PathVariable String provider
    ){
        String normalizedProvider = provider.toLowerCase();

        if(!SUPPORT_PROVIDERS.contains(normalizedProvider)){
            throw UserException.unsupportedOAuthProvider();
        }

        URI redirectUri = URI.create("/oauth2/authorization/" + normalizedProvider);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }

}
