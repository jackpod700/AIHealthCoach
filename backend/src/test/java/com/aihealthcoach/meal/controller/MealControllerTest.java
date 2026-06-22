package com.aihealthcoach.meal.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aihealthcoach.common.auth.JwtAccessDeniedHandler;
import com.aihealthcoach.common.auth.JwtAuthenticationEntryPoint;
import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.common.config.SecurityConfig;
import com.aihealthcoach.meal.exception.MealException;
import com.aihealthcoach.meal.service.MealService;

@WebMvcTest(MealController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class,
        com.aihealthcoach.common.config.OAuthWebMvcTestConfig.class})
class MealControllerTest {

    private static final String TOKEN = "access-token";
    private static final Long USER_ID = 1L;
    private static final Long MEAL_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MealService mealService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisRepository tokenRedisRepository;

    @Test
    void deleteMealReturnsNoContent() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(delete("/api/meals/{mealId}", MEAL_ID)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent());

        verify(mealService).deleteMeal(USER_ID, MEAL_ID);
    }

    @Test
    void deleteMealReturnsNotFoundForMissingOrUnownedMeal() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);
        doThrow(MealException.mealNotFound()).when(mealService).deleteMeal(USER_ID, MEAL_ID);

        mockMvc.perform(delete("/api/meals/{mealId}", MEAL_ID)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEAL_NOT_FOUND"));
    }
}
