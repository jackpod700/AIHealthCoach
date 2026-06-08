package com.aihealthcoach.exercise.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aihealthcoach.common.auth.JwtAccessDeniedHandler;
import com.aihealthcoach.common.auth.JwtAuthenticationEntryPoint;
import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.common.config.SecurityConfig;
import com.aihealthcoach.exercise.service.ExerciseService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExerciseController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class ExerciseControllerTest {

    private static final String TOKEN = "access-token";
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExerciseService exerciseService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisRepository tokenRedisRepository;

    @Test
    void findExerciseActivityOptionsReturnsEmptyListWhenNoResult() throws Exception {
        String keyword = "없는운동";
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);
        when(exerciseService.findExerciseActivityOptions(keyword)).thenReturn(List.of());

        mockMvc.perform(get("/api/exercise/physical-activities")
                        .param("keyword", keyword)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.length()", is(0)));
    }

    @Test
    void findExerciseRecordsByDateReturnsBadRequestForInvalidDateFormat() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(get("/api/exercise/records")
                        .param("date", "2026-06")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_ERROR")));

        verifyNoInteractions(exerciseService);
    }

    @Test
    void findExerciseDatesInMonthReturnsBadRequestForInvalidMonth() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(get("/api/exercise/records/calendar")
                        .param("year", "2026")
                        .param("month", "13")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_ERROR")));
    }

    @Test
    void findExerciseDatesInMonthReturnsBadRequestForInvalidYearFormat() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(get("/api/exercise/records/calendar")
                        .param("year", "abc")
                        .param("month", "6")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_ERROR")));

        verifyNoInteractions(exerciseService);
    }
}
