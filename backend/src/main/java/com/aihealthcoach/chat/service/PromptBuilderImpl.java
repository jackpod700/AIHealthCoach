package com.aihealthcoach.chat.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.aihealthcoach.chat.dto.ChatContextDto.UserChatContext;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.dto.LlmDto.LlmImage;
import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.meal.dto.MealDto.MealResponse;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryResponse;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromptBuilderImpl implements PromptBuilder {

    private final AiPromptFactory promptFactory;

    @Override
    public LlmRequest buildText(LocalDate contextDate, String userMessage, UserChatContext context) {
        return LlmRequest.text(
                promptFactory.textChatPrompt(),
                renderDynamicContext(contextDate, context),
                userMessage
        );
    }

    @Override
    public LlmRequest buildImage(
            LocalDate contextDate,
            String userMessage,
            List<LlmImage> images,
            UserChatContext context
    ) {
        return LlmRequest.image(
                promptFactory.imageMealPrompt(),
                renderDynamicContext(contextDate, context),
                userMessage,
                images
        );
    }

    String renderDynamicContext(LocalDate contextDate, UserChatContext context) {
        StringBuilder prompt = new StringBuilder();
        appendSection(prompt, "reference_date", contextDate.toString());
        appendProfile(prompt, context);
        appendDailyGoal(prompt, context);
        appendDailyMeals(prompt, context);
        appendDailyExercises(prompt, context);
        appendRecentDailySummaries(prompt, context);
        appendRecentTurns(prompt, context);
        appendActiveMemories(prompt, context);
        return prompt.toString().trim();
    }

    private void appendProfile(StringBuilder prompt, UserChatContext context) {
        if (context == null || context.profile() == null) {
            return;
        }

        List<String> values = new ArrayList<>();
        addValue(values, "heightCm", context.profile().heightCm());
        addValue(values, "currentWeightKg", context.profile().currentWeightKg());
        addValue(values, "targetWeightKg", context.profile().targetWeightKg());
        addValue(values, "goalType", context.profile().goalType());
        addValue(values, "gender", context.profile().gender());
        addValue(values, "age", context.profile().age());
        if (!values.isEmpty()) {
            appendSection(prompt, "user_profile", String.join(", ", values));
        }
    }

    private void appendDailyGoal(StringBuilder prompt, UserChatContext context) {
        if (context == null || context.dailyGoal() == null) {
            return;
        }

        List<String> values = new ArrayList<>();
        addValue(values, "goalType", context.dailyGoal().goalType());
        addValue(values, "calorieIntakeGoal", context.dailyGoal().calorieIntakeGoal());
        addValue(values, "exerciseCalorieGoal", context.dailyGoal().exerciseCalorieGoal());
        if (!values.isEmpty()) {
            appendSection(prompt, "daily_goal", String.join(", ", values));
        }
    }

    private void appendDailyMeals(StringBuilder prompt, UserChatContext context) {
        if (context == null || context.dailyMeals() == null || context.dailyMeals().meals() == null
                || context.dailyMeals().meals().isEmpty()) {
            return;
        }

        String meals = context.dailyMeals().meals().stream()
                .map(this::formatMeal)
                .collect(Collectors.joining("\n"));
        appendSection(prompt, "daily_meals", meals);
    }

    private void appendDailyExercises(StringBuilder prompt, UserChatContext context) {
        if (context == null || context.dailyExercises().isEmpty()) {
            return;
        }

        String exercises = context.dailyExercises().stream()
                .map(this::formatExercise)
                .collect(Collectors.joining("\n"));
        appendSection(prompt, "daily_exercises", exercises);
    }

    private void appendRecentTurns(StringBuilder prompt, UserChatContext context) {
        if (context == null || context.recentTurns().isEmpty()) {
            return;
        }

        String turns = context.recentTurns().stream()
                .map(this::formatTurn)
                .collect(Collectors.joining("\n"));
        appendSection(prompt, "recent_chat_turns", turns);
    }

    private void appendRecentDailySummaries(StringBuilder prompt, UserChatContext context) {
        if (context == null || context.recentDailySummaries().isEmpty()) {
            return;
        }

        String summaries = context.recentDailySummaries().stream()
                .map(this::formatDailySummary)
                .collect(Collectors.joining("\n"));
        appendSection(prompt, "recent_daily_summaries", summaries);
    }

    private void appendActiveMemories(StringBuilder prompt, UserChatContext context) {
        if (context == null || context.activeMemories().isEmpty()) {
            return;
        }

        String memories = context.activeMemories().stream()
                .map(UserMemoryResponse::content)
                .map(this::escapeXml)
                .map(content -> "- " + content)
                .collect(Collectors.joining("\n"));
        appendSection(prompt, "user_memories", memories);
    }

    private String formatMeal(MealResponse meal) {
        String items = meal.items() == null ? "" : meal.items().stream()
                .map(item -> escapeXml(item.foodName()) + " x" + item.quantity())
                .collect(Collectors.joining(", "));
        return "- " + escapeXml(meal.mealType()) + ": " + items;
    }

    private String formatExercise(ExerciseRecordResponse exercise) {
        List<String> values = new ArrayList<>();
        addValue(values, "activity", exercise.activityNameKo());
        addValue(values, "intensity", exercise.intensityLevel());
        addValue(values, "durationMinutes", exercise.durationMinutes());
        addValue(values, "caloriesBurned", exercise.caloriesBurned());
        addValue(values, "memo", exercise.memo());
        return "- " + String.join(", ", values);
    }

    private String formatTurn(ChatMessageResponse turn) {
        return "- " + escapeXml(turn.role()) + ": " + escapeXml(turn.content());
    }

    private String formatDailySummary(DailyChatSummaryContextResponse summary) {
        return "- " + summary.summaryDate() + ": " + escapeXml(summary.content());
    }

    private void appendSection(StringBuilder prompt, String sectionName, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        if (!prompt.isEmpty()) {
            prompt.append("\n\n");
        }
        prompt.append("<").append(sectionName).append(">\n")
                .append(content)
                .append("\n</").append(sectionName).append(">");
    }

    private void addValue(List<String> values, String name, Object value) {
        if (value != null && !value.toString().isBlank()) {
            values.add(name + "=" + escapeXml(value.toString()));
        }
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
