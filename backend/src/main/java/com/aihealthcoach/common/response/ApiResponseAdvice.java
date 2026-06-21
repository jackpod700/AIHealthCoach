package com.aihealthcoach.common.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.aihealthcoach.common.error.GlobalExceptionHandler;

@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    /*
     * 컨트롤러가 DTO 또는 ResponseEntity<DTO>를 반환하면 Spring MVC가 응답 본문을 쓰기 직전에
     * 이 Advice를 호출한다. supports()에서 적용 대상을 고른 뒤, beforeBodyWrite()에서 실제
     * body를 ApiResponse.success(body)로 감싸 공통 응답 형식으로 내려보낸다.
     *
     * 예외 응답은 GlobalExceptionHandler에서 ApiResponse.error(...)로 직접 만들고,
     * 204 No Content 같은 빈 응답은 body가 null이므로 그대로 통과한다.
     *
     * 컨트롤러 반환 방식별 동작:
     * - DTO만 반환하면 기본 상태 코드는 200 OK이고, DTO가 data에 들어간다.
     * - ResponseEntity<DTO>를 반환하면 ResponseEntity의 상태 코드는 유지하고, body의 DTO만 data에 넣는다.
     * - ResponseEntity.noContent().build()처럼 body가 없으면 감싸지 않고 204 No Content로 그대로 응답한다.
     *
     * Swagger UI는 /v3/api-docs 응답을 OpenAPI 원본 JSON 그대로 읽어야 하므로 공통 응답으로 감싸지 않는다.
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 예외 핸들러는 이미 ApiResponse.error(...)를 만들기 때문에 다시 감싸지 않는다.
        return !GlobalExceptionHandler.class.equals(returnType.getContainingClass());
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (isSpringDocRequest(request) || isActuatorRequest(request)) {
            return body;
        }

        // 빈 응답, 이미 감싼 응답, 문자열 응답은 그대로 둔다.
        // 문자열 응답은 StringHttpMessageConverter를 사용하므로 여기서 객체로 바꾸면 실패할 수 있다.
        if (body == null || body instanceof ApiResponse<?> || body instanceof String || body instanceof byte[]) {
            return body;
        }

        // 일반 컨트롤러 DTO 응답은 { success: true, data: ... } 형태로 감싼다.
        return ApiResponse.success(body);
    }

    private boolean isSpringDocRequest(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html");
    }

    private boolean isActuatorRequest(ServerHttpRequest request) {
        return request.getURI().getPath().startsWith("/actuator");
    }
}
