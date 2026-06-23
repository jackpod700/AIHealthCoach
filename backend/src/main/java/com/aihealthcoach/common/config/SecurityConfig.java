package com.aihealthcoach.common.config;

import com.aihealthcoach.admin.filter.ActiveUserMetricsFilter;
import com.aihealthcoach.admin.service.ActiveUserMetricsService;
import com.aihealthcoach.common.auth.JwtAccessDeniedHandler;
import com.aihealthcoach.common.auth.JwtAuthenticationEntryPoint;
import com.aihealthcoach.common.auth.JwtAuthenticationFilter;
import com.aihealthcoach.common.auth.SecurityPaths;
import com.aihealthcoach.user.oauth.OAuth2LoginFailureHandler;
import com.aihealthcoach.user.oauth.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final ObjectProvider<OAuth2LoginSuccessHandler> oAuth2LoginSuccessHandlerProvider;
    private final ObjectProvider<OAuth2LoginFailureHandler> oAuth2LoginFailureHandlerProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ActiveUserMetricsFilter activeUserMetricsFilter
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/user/*/profile").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/user/*/profile").authenticated()
                        .requestMatchers(
                                "/api/oauth/**",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/user/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/user/nickname").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/user/profile").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/user/profile").authenticated()
                        .requestMatchers(SecurityPaths.PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated()
                );

        OAuth2LoginSuccessHandler successHandler = oAuth2LoginSuccessHandlerProvider.getIfAvailable();
        OAuth2LoginFailureHandler failureHandler = oAuth2LoginFailureHandlerProvider.getIfAvailable();
        if (successHandler != null && failureHandler != null) {
            http.oauth2Login(oauth -> oauth
                    .successHandler(successHandler)
                    .failureHandler(failureHandler)
            );
        }

        return http
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(activeUserMetricsFilter, JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    public ActiveUserMetricsFilter activeUserMetricsFilter(
            ObjectProvider<ActiveUserMetricsService> activeUserMetricsServiceProvider
    ) {
        return new ActiveUserMetricsFilter(activeUserMetricsServiceProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
