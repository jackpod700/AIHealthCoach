package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;

import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;

@ExtendWith(MockitoExtension.class)
class LlmServiceImplTest {

    @Mock
    private AiChatClientGateway aiChatClientGateway;
    @Mock
    private ResponseEntity<ChatResponse, String> responseEntity;

    @Test
    void generateSendsTheRenderedDynamicContextToTheProvider() {
        LlmRequest request = LlmRequest.text("stable prompt", "<user_memories>\n- 유제품 회피\n</user_memories>", "저녁 추천해줘");
        when(aiChatClientGateway.callTextChat(request.systemPrompt(), "저녁 추천해줘"))
                .thenReturn(responseEntity);
        when(responseEntity.entity()).thenReturn("{\"assistantMessage\":\"알겠어요.\"}");
        LlmServiceImpl service = new LlmServiceImpl(aiChatClientGateway);

        String content = service.generate(request).content();

        assertThat(content).contains("assistantMessage");
        verify(aiChatClientGateway).callTextChat(
                "stable prompt\n\n<user_memories>\n- 유제품 회피\n</user_memories>",
                "저녁 추천해줘"
        );
    }
}
