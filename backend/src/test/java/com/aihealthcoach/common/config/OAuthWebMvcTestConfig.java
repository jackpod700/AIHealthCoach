package com.aihealthcoach.common.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.user.oauth.OAuth2LoginFailureHandler;
import com.aihealthcoach.user.oauth.OAuth2LoginSuccessHandler;
import com.aihealthcoach.user.service.OAuthService;

@TestConfiguration(proxyBeanMethods = false)
public class OAuthWebMvcTestConfig {

    @Bean
    OAuthService oauthService() {
        return Mockito.mock(OAuthService.class);
    }

    @Bean
    OAuth2LoginSuccessHandler oauth2LoginSuccessHandler(
            OAuthService oauthService,
            JwtTokenProvider jwtTokenProvider,
            TokenRedisRepository tokenRedisRepository
    ) {
        return new OAuth2LoginSuccessHandler(oauthService, jwtTokenProvider, tokenRedisRepository);
    }

    @Bean
    OAuth2LoginFailureHandler oauth2LoginFailureHandler() {
        return new OAuth2LoginFailureHandler();
    }
}
