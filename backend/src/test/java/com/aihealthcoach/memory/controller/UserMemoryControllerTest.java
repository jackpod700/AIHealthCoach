package com.aihealthcoach.memory.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aihealthcoach.common.auth.JwtAccessDeniedHandler;
import com.aihealthcoach.common.auth.JwtAuthenticationEntryPoint;
import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.common.config.SecurityConfig;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryCreateRequest;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryResponse;
import com.aihealthcoach.memory.service.UserMemoryService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserMemoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class,
        com.aihealthcoach.common.config.OAuthWebMvcTestConfig.class})
class UserMemoryControllerTest {

    private static final String TOKEN = "access-token";
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserMemoryService userMemoryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisRepository tokenRedisRepository;

    @Test
    void createMemoryUsesTheAuthenticatedUserId() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);
        when(userMemoryService.createMemory(eq(USER_ID), org.mockito.ArgumentMatchers.any(UserMemoryCreateRequest.class)))
                .thenReturn(new UserMemoryResponse(
                        10L,
                        "유제품은 피하고 싶어",
                        true,
                        LocalDateTime.of(2026, 6, 20, 12, 0),
                        LocalDateTime.of(2026, 6, 20, 12, 0)
                ));

        mockMvc.perform(post("/api/user-memories")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "유제품은 피하고 싶어"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(10)))
                .andExpect(jsonPath("$.data.content", is("유제품은 피하고 싶어")));

        verify(userMemoryService).createMemory(eq(USER_ID), org.mockito.ArgumentMatchers.any(UserMemoryCreateRequest.class));
    }

    @Test
    void findMemoriesUsesTheAuthenticatedUserId() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);
        when(userMemoryService.findMemoriesByUserId(USER_ID)).thenReturn(List.of(
                new UserMemoryResponse(
                        10L,
                        "유제품은 피하고 싶어",
                        true,
                        LocalDateTime.of(2026, 6, 20, 12, 0),
                        LocalDateTime.of(2026, 6, 20, 12, 0)
                )
        ));

        mockMvc.perform(get("/api/user-memories")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].content", is("유제품은 피하고 싶어")))
                .andExpect(jsonPath("$.data[0].active", is(true)));

        verify(userMemoryService).findMemoriesByUserId(USER_ID);
    }

    @Test
    void createMemoryRejectsBlankContent() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(post("/api/user-memories")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void createMemoryRejectsContentOverFiveHundredCharacters() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(post("/api/user-memories")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void deactivateMemoryUsesTheAuthenticatedUserId() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(delete("/api/user-memories/{memoryId}", 10L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.message", is("해당 사용자 메모리가 비활성화되었습니다.")));

        verify(userMemoryService).deactivateMemory(USER_ID, 10L);
    }
}
