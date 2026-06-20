package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;

@ExtendWith(MockitoExtension.class)
class LlmServiceImplTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Test
    void generateSendsTheRenderedDynamicContextToTheProvider() {
        LlmRequest request = LlmRequest.text("stable prompt", "<user_memories>\n- 유제품 회피\n</user_memories>", "저녁 추천해줘");
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.user("저녁 추천해줘")).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("{\"assistantMessage\":\"알겠어요.\"}");
        LlmServiceImpl service = new LlmServiceImpl(chatClient);

        String content = service.generate(request).content();

        assertThat(content).contains("assistantMessage");
        verify(chatClient).prompt("stable prompt\n\n<user_memories>\n- 유제품 회피\n</user_memories>");
    }
}
