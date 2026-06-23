package com.aihealthcoach.common.auth;

public final class SecurityPaths {

    private SecurityPaths() {
    }

    public static final String[] PUBLIC_PATHS = {
            "/api/health",
            "/api/user/signup",
            "/api/user/login",
            "/api/user/token/refresh",
            "/api/fatsecret/test/**",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };
}
