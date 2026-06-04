package com.aihealthcoach.common.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.aihealthcoach.common.response.ApiResponse;
import com.aihealthcoach.user.exception.UserErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /*
     * Spring Security 인증 실패 응답을 담당한다.
     *
     * 컨트롤러에 도달하기 전에 토큰이 없거나 잘못된 경우에는 GlobalExceptionHandler가 아니라
     * Security의 AuthenticationEntryPoint가 호출된다. 여기서 상태 코드, Content-Type, 응답 body를
     * 직접 작성해 JWT 인증 실패도 ApiResponse 형식으로 통일한다.
     */
    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        UserErrorCode errorCode = UserErrorCode.INVALID_TOKEN;

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(errorCode.name(), errorCode.getMessage())
        );
    }
}
