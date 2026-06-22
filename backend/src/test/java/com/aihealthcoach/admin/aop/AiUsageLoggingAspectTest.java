package com.aihealthcoach.admin.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;

import com.aihealthcoach.admin.service.AiUsageMetricsService;

class AiUsageLoggingAspectTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void recordsSuccessWhenAiGatewayReturnsCallResponse() throws Throwable {
        AiUsageMetricsService metricsService = mock(AiUsageMetricsService.class);
        AiUsageLoggingAspect aspect = new AiUsageLoggingAspect(metricsService);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        AiUsageTracked annotation = mock(AiUsageTracked.class);
        ChatResponse chatResponse = mock(ChatResponse.class);
        ResponseEntity result = mock(ResponseEntity.class);

        when(annotation.requestType()).thenReturn("TEXT_CHAT");
        when(joinPoint.proceed()).thenReturn(result);
        when(result.response()).thenReturn(chatResponse);

        Object actual = aspect.recordAiUsage(joinPoint, annotation);

        assertThat(actual).isSameAs(result);
        verify(metricsService).recordSuccess(eq("TEXT_CHAT"), anyLong(), eq(chatResponse));
    }

    @Test
    void recordsFailureAndRethrowsOriginalException() throws Throwable {
        AiUsageMetricsService metricsService = mock(AiUsageMetricsService.class);
        AiUsageLoggingAspect aspect = new AiUsageLoggingAspect(metricsService);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        AiUsageTracked annotation = mock(AiUsageTracked.class);
        RuntimeException exception = new RuntimeException("AI failed");

        when(annotation.requestType()).thenReturn("IMAGE_MEAL");
        when(joinPoint.proceed()).thenThrow(exception);

        assertThatThrownBy(() -> aspect.recordAiUsage(joinPoint, annotation))
                .isSameAs(exception);

        verify(metricsService).recordFailure(eq("IMAGE_MEAL"), anyLong());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void recordsSuccessWithoutUserIdArgument() throws Throwable {
        AiUsageMetricsService metricsService = mock(AiUsageMetricsService.class);
        AiUsageLoggingAspect aspect = new AiUsageLoggingAspect(metricsService);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        AiUsageTracked annotation = mock(AiUsageTracked.class);
        ChatResponse chatResponse = mock(ChatResponse.class);
        ResponseEntity result = mock(ResponseEntity.class);

        when(annotation.requestType()).thenReturn("TEXT_CHAT");
        when(joinPoint.proceed()).thenReturn(result);
        when(result.response()).thenReturn(chatResponse);

        Object actual = aspect.recordAiUsage(joinPoint, annotation);

        assertThat(actual).isSameAs(result);
        verify(metricsService).recordSuccess(eq("TEXT_CHAT"), anyLong(), eq(chatResponse));
    }
}
