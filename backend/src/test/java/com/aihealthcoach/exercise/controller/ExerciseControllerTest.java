package com.aihealthcoach.exercise.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aihealthcoach.common.auth.JwtAccessDeniedHandler;
import com.aihealthcoach.common.auth.JwtAuthenticationEntryPoint;
import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.common.config.SecurityConfig;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordRequest;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.exercise.service.ExerciseService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExerciseController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class,
        com.aihealthcoach.common.config.OAuthWebMvcTestConfig.class})
class ExerciseControllerTest {

    private static final String TOKEN = "access-token";
    private static final Long USER_ID = 1L;
    private static final Long RECORD_ID = 10L;

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
    void updateExerciseRecordUsesAuthenticatedUserAndRecordId() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);
        when(exerciseService.updateExerciseRecord(eq(USER_ID), eq(RECORD_ID), any(ExerciseRecordRequest.class)))
                .thenReturn(ExerciseRecordResponse.builder()
                        .id(RECORD_ID)
                        .exerciseActivityOptionId(1L)
                        .activityNameKo("걷기")
                        .intensityLevel("MEDIUM")
                        .exerciseDate(LocalDate.of(2026, 6, 2))
                        .durationMinutes(30)
                        .caloriesBurned(133)
                        .build());

        mockMvc.perform(put("/api/exercise/records/{recordId}", RECORD_ID)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exerciseActivityOptionId": 1,
                                  "intensityLevel": "MEDIUM",
                                  "exerciseDate": "2026-06-02",
                                  "durationMinutes": 30,
                                  "memo": "퇴근 후 걷기"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(10)))
                .andExpect(jsonPath("$.data.activityNameKo", is("걷기")));

        verify(exerciseService).updateExerciseRecord(eq(USER_ID), eq(RECORD_ID), any(ExerciseRecordRequest.class));
    }

    @Test
    void deleteExerciseRecordUsesAuthenticatedUserAndRecordId() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(delete("/api/exercise/records/{recordId}", RECORD_ID)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent());

        verify(exerciseService).deleteExerciseRecord(USER_ID, RECORD_ID);
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
