package com.aihealthcoach.weight.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordRequest;
import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordResponse;
import com.aihealthcoach.weight.service.WeightRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WeightRecordController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class,
        com.aihealthcoach.common.config.OAuthWebMvcTestConfig.class})
class WeightRecordControllerTest {
    private static final String TOKEN = "access-token";
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeightRecordService weightRecordService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisRepository tokenRedisRepository;

    @Test
    void upsertWeightRecordReturnsApiResponseWrappedBody() throws Exception {
        LocalDate recordDate = LocalDate.of(2026, 6, 17);
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);
        when(weightRecordService.upsertWeightRecord(
                eq(USER_ID),
                org.mockito.ArgumentMatchers.any(WeightRecordRequest.class)
        )).thenReturn(new WeightRecordResponse(
                recordDate,
                new BigDecimal("68.40"),
                LocalDateTime.of(2026, 6, 17, 12, 0)
        ));

        mockMvc.perform(put("/api/weight-records")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordDate": "2026-06-17",
                                  "weightKg": 68.40
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.recordDate", is("2026-06-17")))
                .andExpect(jsonPath("$.data.weightKg", is(68.40)))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    void findWeightRecordsReturnsApiResponseWrappedBody() throws Exception {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);
        when(weightRecordService.findWeightRecords(USER_ID, from, to)).thenReturn(List.of(new WeightRecordResponse(
                LocalDate.of(2026, 6, 17),
                new BigDecimal("68.40"),
                LocalDateTime.of(2026, 6, 17, 12, 0)
        )));

        mockMvc.perform(get("/api/weight-records")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].recordDate", is("2026-06-17")))
                .andExpect(jsonPath("$.data[0].weightKg", is(68.40)));
    }

    @Test
    void deleteWeightRecordUsesAuthenticatedUserAndDate() throws Exception {
        LocalDate recordDate = LocalDate.of(2026, 6, 17);
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(USER_ID);

        mockMvc.perform(delete("/api/weight-records")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordDate": "2026-06-17"
                                }
                                """))
                .andExpect(status().isOk());

        verify(weightRecordService).deleteWeightRecord(USER_ID, recordDate);
    }
}
