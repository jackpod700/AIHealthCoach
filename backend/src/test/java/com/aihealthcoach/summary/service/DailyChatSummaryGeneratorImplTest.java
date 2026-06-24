package com.aihealthcoach.summary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import com.aihealthcoach.chat.dto.LlmDto.LlmResponse;
import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.service.LlmService;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalMetricProgressResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressSummaryResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalMacroRatioResponse;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.meal.dto.MealDto.MealResponse;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummarySourceData;
import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordResponse;

@ExtendWith(MockitoExtension.class)
class DailyChatSummaryGeneratorImplTest {

    @Mock
    private LlmService llmService;

    @Test
    void generateOmitsEmptySourceSectionsFromDynamicPrompt() {
        DailyChatSummaryGeneratorImpl generator = new DailyChatSummaryGeneratorImpl(llmService);
        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        when(llmService.generate(requestCaptor.capture())).thenReturn(new LlmResponse("요약"));

        generator.generate(new DailyChatSummarySourceData(
                1L,
                LocalDate.of(2026, 6, 21),
                "CHAT",
                null,
                null,
                List.of(),
                new DailyMealResponse(LocalDate.of(2026, 6, 21), List.of(), null, null, null, null),
                List.of(),
                null
        ));

        String dynamicPrompt = requestCaptor.getValue().dynamicContextPrompt();
        assertThat(dynamicPrompt).contains("기준일:\n2026-06-21");
        assertThat(dynamicPrompt).contains("변경 source:\nCHAT");
        assertThat(dynamicPrompt).doesNotContain("없음");
        assertThat(dynamicPrompt).doesNotContain("목표 변경 snapshot:");
        assertThat(dynamicPrompt).doesNotContain("몸무게 기록:");
        assertThat(dynamicPrompt).doesNotContain("식사 기록:");
        assertThat(dynamicPrompt).doesNotContain("운동 기록:");
        assertThat(dynamicPrompt).doesNotContain("채팅 기록:");
    }

    @Test
    void generateBuildsPromptWithGoalAchievementRelevantSections() {
        DailyChatSummaryGeneratorImpl generator = new DailyChatSummaryGeneratorImpl(llmService);
        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        when(llmService.generate(requestCaptor.capture())).thenReturn(new LlmResponse("요약"));

        generator.generate(new DailyChatSummarySourceData(
                1L,
                LocalDate.of(2026, 6, 21),
                "CHAT,DAILY_GOAL,EXERCISE,MEAL,WEIGHT",
                "{\"goalType\":\"WEIGHT_LOSS\",\"calorieIntakeGoal\":1600,\"exerciseCalorieGoal\":300}",
                new DailyGoalProgressResponse(
                        LocalDate.of(2026, 6, 21),
                        new DailyGoalProgressSummaryResponse(
                                new DailyGoalMetricProgressResponse(
                                        new BigDecimal("650.0"),
                                        new BigDecimal("1600"),
                                        new BigDecimal("950.0"),
                                        41
                                ),
                                new DailyGoalMetricProgressResponse(
                                        new BigDecimal("180"),
                                        new BigDecimal("300"),
                                        new BigDecimal("120"),
                                        60
                                )
                        ),
                        new DailyGoalMacroRatioResponse(null, null, null)
                ),
                List.of(ChatMessage.builder()
                        .userId(1L)
                        .role("USER")
                        .content("오늘은 목표를 맞춰보고 싶어")
                        .createdAt(LocalDateTime.of(2026, 6, 21, 9, 0))
                        .build()),
                new DailyMealResponse(
                        LocalDate.of(2026, 6, 21),
                        List.of(new MealResponse(
                                10L,
                                "LUNCH",
                                List.of(),
                                new BigDecimal("650.0"),
                                new BigDecimal("80.0"),
                                new BigDecimal("35.0"),
                                new BigDecimal("18.0")
                        )),
                        new BigDecimal("650.0"),
                        new BigDecimal("80.0"),
                        new BigDecimal("35.0"),
                        new BigDecimal("18.0")
                ),
                List.of(ExerciseRecordResponse.builder()
                        .id(20L)
                        .activityNameKo("걷기")
                        .durationMinutes(40)
                        .caloriesBurned(180)
                        .memo("퇴근 후 산책")
                        .build()),
                new WeightRecordResponse(
                        LocalDate.of(2026, 6, 21),
                        new BigDecimal("68.40"),
                        LocalDateTime.of(2026, 6, 21, 8, 0)
                )
        ));

        LlmRequest request = requestCaptor.getValue();
        assertThat(request.stableSystemPrompt()).contains("목표 대비 달성 여부");
        assertThat(request.userMessage()).isEqualTo("위 하루 기록을 daily summary로 요약해줘.");
        assertThat(request.dynamicContextPrompt()).isEqualTo("""
                기준일:
                2026-06-21

                변경 source:
                CHAT,DAILY_GOAL,EXERCISE,MEAL,WEIGHT

                목표 변경 snapshot:
                {"goalType":"WEIGHT_LOSS","calorieIntakeGoal":1600,"exerciseCalorieGoal":300}

                일일 목표 진행 상황:
                섭취: 650 / 1600 kcal, 남은 섭취 950 kcal, 달성률 41%
                운동: 180 / 300 kcal, 남은 운동 120 kcal, 달성률 60%

                몸무게 기록:
                68.40 kg

                식사 기록:
                - LUNCH: 650.0 kcal, 탄수화물 80.0g, 단백질 35.0g, 지방 18.0g

                운동 기록:
                - 걷기 40분, 180 kcal, 메모: 퇴근 후 산책

                채팅 기록:
                - USER: 오늘은 목표를 맞춰보고 싶어
                """.trim());
    }
}
