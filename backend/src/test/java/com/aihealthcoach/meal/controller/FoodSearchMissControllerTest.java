package com.aihealthcoach.meal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aihealthcoach.common.auth.JwtAccessDeniedHandler;
import com.aihealthcoach.common.auth.JwtAuthenticationEntryPoint;
import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.common.config.SecurityConfig;
import com.aihealthcoach.meal.dto.FoodSearchMissDto.FoodSearchMissCreateRequest;
import com.aihealthcoach.meal.dto.FoodSearchMissDto.FoodSearchMissResponse;
import com.aihealthcoach.meal.service.FoodSearchMissService;

@WebMvcTest(FoodSearchMissController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class FoodSearchMissControllerTest {

    private static final String TOKEN = "access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FoodSearchMissService foodSearchMissService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisRepository tokenRedisRepository;

    @Test
    void recordMissStoresNoResultQueryForAuthenticatedUser() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(1L);
        when(foodSearchMissService.recordMiss(eq(1L), any(FoodSearchMissCreateRequest.class)))
                .thenReturn(new FoodSearchMissResponse(true, "PENDING", 10L, "apple", "apple", 1));

        mockMvc.perform(post("/api/foods/search-misses")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "apple"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.searchMissId").value(10))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(foodSearchMissService).recordMiss(eq(1L), any(FoodSearchMissCreateRequest.class));
    }
}
