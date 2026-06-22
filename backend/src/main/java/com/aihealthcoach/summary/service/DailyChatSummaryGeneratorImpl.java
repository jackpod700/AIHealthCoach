package com.aihealthcoach.summary.service;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import com.aihealthcoach.chat.service.LlmService;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressResponse;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.meal.dto.MealDto.MealResponse;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummarySourceData;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DailyChatSummaryGeneratorImpl implements DailyChatSummaryGenerator {

    private static final String STABLE_PROMPT = """
            너는 건강 기록을 하루 단위로 요약하는 assistant다.
            제공된 서버 기록만 근거로 사용하고, 없는 수치나 사건을 만들지 않는다.
            출력은 한국어 plain text로 3~6문장 이내로 작성한다.
            식사, 운동, 몸무게, 목표 변경 중 실제 기록이 있는 내용만 간결하게 요약한다.
            일일 목표 진행 상황이 제공되면 목표 대비 달성 여부를 함께 요약한다.
            목표나 기록이 부족하면 달성 여부를 추측하지 않는다.
            """;

    private final LlmService llmService;

    @Override
    public String generate(DailyChatSummarySourceData sourceData) {
        String content = llmService.generate(LlmRequest.text(
                STABLE_PROMPT,
                buildDynamicPrompt(sourceData),
                "위 하루 기록을 daily summary로 요약해줘."
        )).content();

        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Daily summary LLM response is blank");
        }

        return content.trim();
    }

    private String buildDynamicPrompt(DailyChatSummarySourceData sourceData) {
        StringBuilder prompt = new StringBuilder();
        appendSection(prompt, "기준일", sourceData.summaryDate().toString());
        appendSection(prompt, "변경 source", sourceData.changedSources());
        appendSection(prompt, "목표 변경 snapshot", sourceData.dailyGoalSnapshotPayload());
        appendSection(prompt, "일일 목표 진행 상황", renderDailyGoalProgress(sourceData.dailyGoalProgress()));
        appendSection(prompt, "몸무게 기록", renderWeight(sourceData));
        appendSection(prompt, "식사 기록", renderMeals(sourceData));
        appendSection(prompt, "운동 기록", renderExercises(sourceData));
        appendSection(prompt, "채팅 기록", renderChatMessages(sourceData));
        return prompt.toString().trim();
    }

    private void appendSection(StringBuilder prompt, String title, String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        if (!prompt.isEmpty()) {
            prompt.append("\n\n");
        }
        prompt.append(title).append(":\n").append(content);
    }

    private String renderWeight(DailyChatSummarySourceData sourceData) {
        return sourceData.weightRecord() == null ? null : "%s kg".formatted(sourceData.weightRecord().weightKg());
    }

    private String renderDailyGoalProgress(DailyGoalProgressResponse progress) {
        if (progress == null) {
            return null;
        }

        return """
                섭취: %s / %s kcal, 남은 섭취 %s kcal, 달성률 %d%%
                운동: %s / %s kcal, 남은 운동 %s kcal, 달성률 %d%%
                """.formatted(
                formatMetric(progress.progress().calorieIntake().current()),
                formatMetric(progress.progress().calorieIntake().goal()),
                formatMetric(progress.progress().calorieIntake().remaining()),
                progress.progress().calorieIntake().percent(),
                formatMetric(progress.progress().exerciseCalories().current()),
                formatMetric(progress.progress().exerciseCalories().goal()),
                formatMetric(progress.progress().exerciseCalories().remaining()),
                progress.progress().exerciseCalories().percent()
        ).trim();
    }

    private String renderMeals(DailyChatSummarySourceData sourceData) {
        if (sourceData.meals() == null || sourceData.meals().meals().isEmpty()) {
            return null;
        }

        return sourceData.meals().meals().stream()
                .map(this::renderMeal)
                .collect(Collectors.joining("\n"));
    }

    private String renderMeal(MealResponse meal) {
        return "- %s: %.1f kcal, 탄수화물 %.1fg, 단백질 %.1fg, 지방 %.1fg".formatted(
                meal.mealType(),
                defaultZero(meal.totalCalories()),
                defaultZero(meal.totalCarbohydrate()),
                defaultZero(meal.totalProtein()),
                defaultZero(meal.totalFat())
        );
    }

    private String renderExercises(DailyChatSummarySourceData sourceData) {
        if (sourceData.exercises().isEmpty()) {
            return null;
        }

        return sourceData.exercises().stream()
                .map(this::renderExercise)
                .collect(Collectors.joining("\n"));
    }

    private String renderExercise(ExerciseRecordResponse exercise) {
        return "- %s %d분, %d kcal%s".formatted(
                exercise.activityNameKo() == null ? "운동" : exercise.activityNameKo(),
                exercise.durationMinutes() == null ? 0 : exercise.durationMinutes(),
                exercise.caloriesBurned() == null ? 0 : exercise.caloriesBurned(),
                exercise.memo() == null || exercise.memo().isBlank() ? "" : ", 메모: " + exercise.memo()
        );
    }

    private String renderChatMessages(DailyChatSummarySourceData sourceData) {
        if (sourceData.chatMessages().isEmpty()) {
            return null;
        }

        return sourceData.chatMessages().stream()
                .map(message -> "- %s: %s".formatted(message.getRole(), message.getContent()))
                .collect(Collectors.joining("\n"));
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatMetric(BigDecimal value) {
        return defaultZero(value).stripTrailingZeros().toPlainString();
    }
}
