package com.aihealthcoach.dailygoal.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aihealthcoach.common.auth.JwtAccessDeniedHandler;
import com.aihealthcoach.common.auth.JwtAuthenticationEntryPoint;
import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.common.config.SecurityConfig;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalMacroRatioMetricResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalMacroRatioResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalMetricProgressResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressSummaryResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalRecommendationResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalUpsertRequest;
import com.aihealthcoach.dailygoal.service.DailyGoalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DailyGoalController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class DailyGoalControllerTest {

    private static final String TOKEN = "access-token";
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DailyGoalService dailyGoalService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisRepository tokenRedisRepository;

    @Test
    void recommendGoalReturnsApiResponseWrappedBody() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);
        when(dailyGoalService.recommendGoal(USER_ID, "WEIGHT_LOSS")).thenReturn(new DailyGoalRecommendationResponse(
                1800,
                300
        ));

        mockMvc.perform(get("/api/daily-goals/recommendation")
                        .param("goalType", "WEIGHT_LOSS")
                .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.goalType").doesNotExist())
                .andExpect(jsonPath("$.data.calorieIntakeGoal", is(1800)))
                .andExpect(jsonPath("$.data.exerciseCalorieGoal", is(300)))
                .andExpect(jsonPath("$.data.warnings").doesNotExist());
    }

    @Test
    void upsertCurrentGoalReturnsApiResponseWrappedBody() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);
        when(dailyGoalService.upsertCurrentGoal(eq(USER_ID), org.mockito.ArgumentMatchers.any(DailyGoalUpsertRequest.class)))
                .thenReturn(new DailyGoalResponse(
                        "WEIGHT_LOSS",
                        1000,
                        1200,
                        LocalDateTime.of(2026, 6, 16, 0, 0)
                ));

        mockMvc.perform(put("/api/daily-goals/confirm")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalType": "WEIGHT_LOSS",
                                  "calorieIntakeGoal": 1000,
                                  "exerciseCalorieGoal": 1200
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.goalType", is("WEIGHT_LOSS")))
                .andExpect(jsonPath("$.data.warnings").doesNotExist());

        verify(dailyGoalService).upsertCurrentGoal(eq(USER_ID), org.mockito.ArgumentMatchers.any(DailyGoalUpsertRequest.class));
    }

    @Test
    void findProgressReturnsApiResponseWrappedBody() throws Exception {
        LocalDate date = LocalDate.of(2026, 6, 16);
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);
        when(dailyGoalService.findProgress(USER_ID, date)).thenReturn(new DailyGoalProgressResponse(
                date,
                new DailyGoalProgressSummaryResponse(
                        new DailyGoalMetricProgressResponse(
                                new BigDecimal("1200"),
                                new BigDecimal("1600"),
                                new BigDecimal("400"),
                                75
                        ),
                        new DailyGoalMetricProgressResponse(
                                new BigDecimal("210"),
                                new BigDecimal("350"),
                                new BigDecimal("140"),
                                60
                        )
                ),
                new DailyGoalMacroRatioResponse(
                        new DailyGoalMacroRatioMetricResponse(new BigDecimal("180"), new BigDecimal("720"), 51, 45, 65, "BALANCED"),
                        new DailyGoalMacroRatioMetricResponse(new BigDecimal("80"), new BigDecimal("320"), 23, 10, 35, "BALANCED"),
                        new DailyGoalMacroRatioMetricResponse(new BigDecimal("40"), new BigDecimal("360"), 26, 20, 35, "BALANCED")
                )
        ));

        mockMvc.perform(get("/api/daily-goals/progress")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("date", "2026-06-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.goal").doesNotExist())
                .andExpect(jsonPath("$.data.progress.calorieIntake.percent", is(75)))
                .andExpect(jsonPath("$.data.progress.exerciseCalories.percent", is(60)))
                .andExpect(jsonPath("$.data.macroRatio.carbohydrate.percent", is(51)))
                .andExpect(jsonPath("$.data.macroRatio.protein.status", is("BALANCED")))
                .andExpect(jsonPath("$.data.warnings").doesNotExist());
    }
}
