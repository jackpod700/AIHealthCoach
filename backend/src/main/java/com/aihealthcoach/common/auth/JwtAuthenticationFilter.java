package com.aihealthcoach.common.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aihealthcoach.user.entity.User;
import com.aihealthcoach.user.exception.UserException;
import com.aihealthcoach.user.mapper.UserMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRedisRepository tokenRedisRepository;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final ObjectProvider<UserMapper> userMapperProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)){
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        Long userId;

        try {
            jwtTokenProvider.validateAccessToken(token);

            String tokenId = jwtTokenProvider.getTokenId(token);
            if (tokenRedisRepository.isAccessTokenBlacklisted(tokenId)) {
                throw UserException.invalidToken();
            }
            
            userId = jwtTokenProvider.getUserId(token);
        } catch (UserException exception) {
            // JWT 검증은 컨트롤러에 도달하기 전 필터 단계에서 수행된다.
            // 따라서 GlobalExceptionHandler 대신 AuthenticationEntryPoint를 통해 공통 에러 응답을 작성한다.
            SecurityContextHolder.clearContext();
            jwtAuthenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(exception.getMessage(), exception)
            );
            return;
        }

            UserMapper userMapper = userMapperProvider.getIfAvailable();
            User user = userMapper == null ? null : userMapper.findUserById(userId);
            String role = user == null || user.getRole() == null || user.getRole().isBlank()
                    ? "USER"
                    : user.getRole();

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

}
