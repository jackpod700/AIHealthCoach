package com.aihealthcoach.common.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aihealthcoach.user.exception.UserException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INVALID_TOKEN_RESPONSE = """
            {"code":"INVALID_TOKEN","message":"올바르지 않은 JWT 토큰입니다."}
            """;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRedisRepository tokenRedisRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();

        for (String publicPath : SecurityPaths.PUBLIC_PATHS) {
            if (pathMatcher.match(publicPath, requestPath)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)){
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length());

        try {
            jwtTokenProvider.validateAccessToken(token);
            String tokenId = jwtTokenProvider.getTokenId(token);

            if (tokenRedisRepository.isAccessTokenBlacklisted(tokenId)) {
                writeInvalidTokenResponse(response);
                return;
            }

            Long userId = jwtTokenProvider.getUserId(token);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UserException exception) {
            writeInvalidTokenResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeInvalidTokenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(INVALID_TOKEN_RESPONSE);
    }
}
