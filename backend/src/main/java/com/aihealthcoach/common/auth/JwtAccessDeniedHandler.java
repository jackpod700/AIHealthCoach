package com.aihealthcoach.common.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.aihealthcoach.common.response.ApiResponse;
import com.aihealthcoach.user.exception.UserErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    /*
     * 인증은 되었지만 접근 권한이 부족한 403 Forbidden 응답을 담당한다.
     *
     * 인증 실패(401)는 JwtAuthenticationEntryPoint가 처리하고, 권한 실패(403)는 이 Handler가 처리한다.
     * 여기서 응답을 직접 작성해 Spring Security 권한 실패도 ApiResponse 형식으로 통일한다.
     */
    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        UserErrorCode errorCode = UserErrorCode.PROFILE_ACCESS_DENIED;

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error(errorCode.name(), errorCode.getMessage())
        );
    }
}
