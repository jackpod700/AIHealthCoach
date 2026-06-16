package com.aihealthcoach.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

import com.aihealthcoach.admin.entity.AiUsageLog;
import com.aihealthcoach.admin.mapper.AdminMapper;

class AiUsageLogServiceTest {

    @Test
    void recordsSuccessWithTokenUsageMetadata() {
        AdminMapper adminMapper = mock(AdminMapper.class);
        AiUsageLogService service = new AiUsageLogService(adminMapper, "configured-model");
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        Usage usage = mock(Usage.class);

        when(response.getMetadata().getModel()).thenReturn("gpt-4o-mini");
        when(response.getMetadata().getUsage()).thenReturn(usage);
        when(usage.getPromptTokens()).thenReturn(11);
        when(usage.getCompletionTokens()).thenReturn(22);
        when(usage.getTotalTokens()).thenReturn(33);

        service.recordSuccess(1L, "TEXT_CHAT", 123L, response);

        ArgumentCaptor<AiUsageLog> captor = ArgumentCaptor.forClass(AiUsageLog.class);
        verify(adminMapper).insertAiUsageLog(captor.capture());
        AiUsageLog log = captor.getValue();

        assertThat(log.getUserId()).isEqualTo(1L);
        assertThat(log.getRequestType()).isEqualTo("TEXT_CHAT");
        assertThat(log.getModel()).isEqualTo("gpt-4o-mini");
        assertThat(log.getLatencyMs()).isEqualTo(123L);
        assertThat(log.getSuccess()).isTrue();
        assertThat(log.getInputTokens()).isEqualTo(11L);
        assertThat(log.getOutputTokens()).isEqualTo(22L);
        assertThat(log.getTotalTokens()).isEqualTo(33L);
    }

    @Test
    void usesConfiguredModelWhenResponseModelIsMissing() {
        AdminMapper adminMapper = mock(AdminMapper.class);
        AiUsageLogService service = new AiUsageLogService(adminMapper, "configured-model");

        service.recordSuccess(1L, "TEXT_CHAT", 123L, null);

        ArgumentCaptor<AiUsageLog> captor = ArgumentCaptor.forClass(AiUsageLog.class);
        verify(adminMapper).insertAiUsageLog(captor.capture());

        assertThat(captor.getValue().getModel()).isEqualTo("configured-model");
        assertThat(captor.getValue().getInputTokens()).isNull();
        assertThat(captor.getValue().getOutputTokens()).isNull();
        assertThat(captor.getValue().getTotalTokens()).isNull();
    }

    @Test
    void recordsFailureWithTruncatedErrorMessage() {
        AdminMapper adminMapper = mock(AdminMapper.class);
        AiUsageLogService service = new AiUsageLogService(adminMapper, "configured-model");
        String longMessage = "x".repeat(1200);

        service.recordFailure(2L, "IMAGE_MEAL", 456L, new RuntimeException(longMessage));

        ArgumentCaptor<AiUsageLog> captor = ArgumentCaptor.forClass(AiUsageLog.class);
        verify(adminMapper).insertAiUsageLog(captor.capture());
        AiUsageLog log = captor.getValue();

        assertThat(log.getSuccess()).isFalse();
        assertThat(log.getErrorMessage()).hasSize(1000);
        assertThat(log.getLatencyMs()).isEqualTo(456L);
    }

    @Test
    void doesNotPropagateMapperFailure() {
        AdminMapper adminMapper = mock(AdminMapper.class);
        AiUsageLogService service = new AiUsageLogService(adminMapper, "configured-model");
        doThrow(new RuntimeException("db down")).when(adminMapper).insertAiUsageLog(any(AiUsageLog.class));

        service.recordSuccess(1L, "TEXT_CHAT", 123L, null);

        verify(adminMapper).insertAiUsageLog(any(AiUsageLog.class));
    }
}
