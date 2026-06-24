package com.aihealthcoach.admin.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import com.aihealthcoach.admin.service.AiUsageMetricsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AiUsageLoggingAspect {

    private final AiUsageMetricsService aiUsageMetricsService;

    /**
     * Spring AOP는 외부에서 Spring bean의 public method로 들어오는 호출만 안정적으로 감싼다.
     * 그래서 실제 ChatClient 호출을 gateway bean에 두고, 이 aspect가 그 경계에서 AI metric을 기록한다.
     */
    @Around("@annotation(aiUsageTracked)")
    public Object recordAiUsage(ProceedingJoinPoint joinPoint, AiUsageTracked aiUsageTracked) throws Throwable {
        long startedAt = System.nanoTime();

        try {
            Object result = joinPoint.proceed();
            recordSuccess(aiUsageTracked, startedAt, result);
            return result;
        } catch (Exception exception) {
            recordFailure(aiUsageTracked, startedAt);
            throw exception;
        }
    }

    private void recordSuccess(AiUsageTracked aiUsageTracked, long startedAt, Object result) {
        if (!(result instanceof ChatResponse chatResponse)) {
            log.warn("AI usage metric skipped because result does not contain ChatResponse metadata.");
            return;
        }

        aiUsageMetricsService.recordSuccess(
                aiUsageTracked.requestType(),
                elapsedMillis(startedAt),
                chatResponse
        );
    }

    private void recordFailure(AiUsageTracked aiUsageTracked, long startedAt) {
        aiUsageMetricsService.recordFailure(
                aiUsageTracked.requestType(),
                elapsedMillis(startedAt)
        );
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
