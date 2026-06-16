package com.aihealthcoach.admin.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import com.aihealthcoach.admin.service.AiUsageLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AiUsageLoggingAspect {

    private final AiUsageLogService aiUsageLogService;

    /**
     * Spring AOP는 bean 바깥에서 들어오는 public 메서드 호출만 안정적으로 감싼다.
     * 그래서 실제 ChatClient 호출을 gateway bean으로 분리하고, 이 aspect가 그 경계를 감싸 사용량을 기록한다.
     */
    @Around("@annotation(aiUsageTracked)")
    public Object recordAiUsage(ProceedingJoinPoint joinPoint, AiUsageTracked aiUsageTracked) throws Throwable {
        Long userId = extractUserId(joinPoint.getArgs());
        long startedAt = System.nanoTime();

        try {
            Object result = joinPoint.proceed();
            recordSuccess(userId, aiUsageTracked, startedAt, result);
            return result;
        } catch (Exception exception) {
            recordFailure(userId, aiUsageTracked, startedAt, exception);
            throw exception;
        }
    }

    private void recordSuccess(Long userId, AiUsageTracked aiUsageTracked, long startedAt, Object result) {
        if (userId == null) {
            log.warn("AI usage log skipped because userId was not found.");
            return;
        }
        if (!(result instanceof ResponseEntity<?, ?> responseEntity)
                || !(responseEntity.response() instanceof ChatResponse chatResponse)) {
            log.warn("AI usage log skipped because result does not contain ChatResponse metadata.");
            return;
        }

        aiUsageLogService.recordSuccess(
                userId,
                aiUsageTracked.requestType(),
                elapsedMillis(startedAt),
                chatResponse
        );
    }

    private void recordFailure(Long userId, AiUsageTracked aiUsageTracked, long startedAt, Exception exception) {
        if (userId == null) {
            log.warn("AI usage failure log skipped because userId was not found.");
            return;
        }

        aiUsageLogService.recordFailure(
                userId,
                aiUsageTracked.requestType(),
                elapsedMillis(startedAt),
                exception
        );
    }

    private Long extractUserId(Object[] args) {
        if (args == null || args.length == 0 || !(args[0] instanceof Long userId)) {
            return null;
        }
        return userId;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
