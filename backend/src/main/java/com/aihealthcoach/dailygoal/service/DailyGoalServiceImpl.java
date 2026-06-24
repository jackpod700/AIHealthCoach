package com.aihealthcoach.dailygoal.service;

import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalMacroRatioMetricResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalMacroRatioResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalMetricProgressResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressSummaryResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalRecommendationResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalUpsertRequest;
import com.aihealthcoach.dailygoal.entity.DailyGoal;
import com.aihealthcoach.dailygoal.exception.DailyGoalException;
import com.aihealthcoach.dailygoal.mapper.DailyGoalMapper;
import com.aihealthcoach.exercise.mapper.ExerciseMapper;
import com.aihealthcoach.meal.entity.MealFood;
import com.aihealthcoach.meal.mapper.MealMapper;
import com.aihealthcoach.user.entity.UserProfile;
import com.aihealthcoach.user.exception.UserException;
import com.aihealthcoach.user.mapper.UserMapper;
import com.aihealthcoach.summary.service.DailyChatSummaryStateService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyGoalServiceImpl implements DailyGoalService {

    private static final List<String> GOAL_TYPES = List.of("WEIGHT_LOSS", "MAINTENANCE", "MUSCLE_GAIN");
    // TODO: Add profile activity level input and replace this fixed factor with the saved value.
    private static final BigDecimal DEFAULT_ACTIVITY_FACTOR = new BigDecimal("1.2");
    private static final int MIN_HEALTHY_CALORIE_INTAKE = 1200;
    private static final BigDecimal CARBOHYDRATE_CALORIES_PER_GRAM = BigDecimal.valueOf(4);
    private static final BigDecimal PROTEIN_CALORIES_PER_GRAM = BigDecimal.valueOf(4);
    private static final BigDecimal FAT_CALORIES_PER_GRAM = BigDecimal.valueOf(9);

    private final DailyGoalMapper dailyGoalMapper;
    private final UserMapper userMapper;
    private final MealMapper mealMapper;
    private final ExerciseMapper exerciseMapper;
    private final DailyChatSummaryStateService dailyChatSummaryStateService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Map<String, DailyGoalRecommendationResponse> recommendGoals(Long userId) {
        UserProfile profile = findRequiredProfile(userId);
        int maintenanceCalories = estimateMaintenanceCalories(profile);
        Map<String, DailyGoalRecommendationResponse> recommendations = new LinkedHashMap<>();

        for (String goalType : GOAL_TYPES) {
            recommendations.put(goalType, buildRecommendation(maintenanceCalories, goalType));
        }

        return recommendations;
    }

    @Override
    @Transactional
    public DailyGoalResponse upsertCurrentGoal(Long userId, DailyGoalUpsertRequest request) {
        validateGoalType(request.goalType());
        DailyGoal savedGoal = dailyGoalMapper.upsert(DailyGoal.builder()
                .userId(userId)
                .goalType(request.goalType())
                .calorieIntakeGoal(request.calorieIntakeGoal())
                .exerciseCalorieGoal(request.exerciseCalorieGoal())
                .build());
        userMapper.updateUserProfileGoalType(userId, savedGoal.getGoalType());
        dailyChatSummaryStateService.markDailyGoalChanged(
                userId,
                LocalDate.now(clock),
                toDailyGoalSnapshotPayload(savedGoal)
        );

        return toGoalResponse(savedGoal);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyGoalResponse findCurrentGoalIfExists(Long userId) {
        DailyGoal dailyGoal = dailyGoalMapper.findByUserId(userId);
        return dailyGoal == null ? null : toGoalResponse(dailyGoal);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyGoalProgressResponse findProgress(Long userId, LocalDate date) {
        DailyGoal dailyGoal = dailyGoalMapper.findByUserId(userId);

        if (dailyGoal == null) {
            throw DailyGoalException.dailyGoalNotFound();
        }

        MealFood dailyNutrition = defaultNutrition(mealMapper.sumDailyNutrition(userId, date));
        BigDecimal calorieIntake = defaultZero(dailyNutrition.getCalories());
        BigDecimal exerciseCalories = BigDecimal.valueOf(defaultZero(exerciseMapper.sumDailyCaloriesBurned(userId, date)));

        return new DailyGoalProgressResponse(
                date,
                new DailyGoalProgressSummaryResponse(
                        toMetricProgress(calorieIntake, dailyGoal.getCalorieIntakeGoal()),
                        toMetricProgress(exerciseCalories, dailyGoal.getExerciseCalorieGoal())
                ),
                toMacroRatio(dailyNutrition)
        );
    }

    private DailyGoalResponse toGoalResponse(DailyGoal dailyGoal) {
        return new DailyGoalResponse(
                dailyGoal.getGoalType(),
                dailyGoal.getCalorieIntakeGoal(),
                dailyGoal.getExerciseCalorieGoal(),
                dailyGoal.getUpdatedAt()
        );
    }

    private String toDailyGoalSnapshotPayload(DailyGoal dailyGoal) {
        return """
                {"goalType":"%s","calorieIntakeGoal":%d,"exerciseCalorieGoal":%d}
                """.formatted(
                dailyGoal.getGoalType(),
                dailyGoal.getCalorieIntakeGoal(),
                dailyGoal.getExerciseCalorieGoal()
        ).trim();
    }

    private UserProfile findRequiredProfile(Long userId) {
        UserProfile profile = userMapper.findUserProfileByUserId(userId);

        if (profile == null) {
            throw UserException.profileNotFound();
        }

        if (profile.getHeightCm() == null
                || profile.getCurrentWeightKg() == null
                || profile.getGender() == null
                || profile.getAge() == null) {
            throw DailyGoalException.profileRequired();
        }

        return profile;
    }

    private int estimateMaintenanceCalories(UserProfile profile) {
        BigDecimal heightComponent = profile.getHeightCm().multiply(BigDecimal.valueOf(6.25));
        BigDecimal weightComponent = profile.getCurrentWeightKg().multiply(BigDecimal.TEN);
        BigDecimal ageComponent = BigDecimal.valueOf(profile.getAge()).multiply(BigDecimal.valueOf(5));
        BigDecimal genderAdjustment = "MALE".equals(profile.getGender())
                ? BigDecimal.valueOf(5)
                : BigDecimal.valueOf(-161);
        BigDecimal basalMetabolicRate = weightComponent
                .add(heightComponent)
                .subtract(ageComponent)
                .add(genderAdjustment);

        return basalMetabolicRate.multiply(DEFAULT_ACTIVITY_FACTOR).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private int adjustCalorieIntakeGoal(int maintenanceCalories, String goalType) {
        return switch (goalType) {
            case "WEIGHT_LOSS" -> Math.max(MIN_HEALTHY_CALORIE_INTAKE, maintenanceCalories - 500);
            case "MAINTENANCE" -> maintenanceCalories;
            case "MUSCLE_GAIN" -> maintenanceCalories + 300;
            default -> throw DailyGoalException.invalidGoalType();
        };
    }

    private int recommendExerciseCalorieGoal(String goalType) {
        return switch (goalType) {
            case "WEIGHT_LOSS" -> 300;
            case "MAINTENANCE" -> 250;
            case "MUSCLE_GAIN" -> 300;
            default -> throw DailyGoalException.invalidGoalType();
        };
    }

    private DailyGoalRecommendationResponse buildRecommendation(int maintenanceCalories, String goalType) {
        return new DailyGoalRecommendationResponse(
                adjustCalorieIntakeGoal(maintenanceCalories, goalType),
                recommendExerciseCalorieGoal(goalType)
        );
    }

    private DailyGoalMetricProgressResponse toMetricProgress(BigDecimal current, Integer goal) {
        BigDecimal goalValue = BigDecimal.valueOf(goal);
        BigDecimal remaining = goalValue.subtract(current).max(BigDecimal.ZERO);
        Integer percent = goal == 0
                ? 100
                : current.multiply(BigDecimal.valueOf(100))
                        .divide(goalValue, 0, RoundingMode.HALF_UP)
                        .intValue();

        return new DailyGoalMetricProgressResponse(current, goalValue, remaining, percent);
    }

    private DailyGoalMacroRatioResponse toMacroRatio(MealFood dailyNutrition) {
        BigDecimal carbohydrateGrams = defaultZero(dailyNutrition.getCarbohydrate());
        BigDecimal proteinGrams = defaultZero(dailyNutrition.getProtein());
        BigDecimal fatGrams = defaultZero(dailyNutrition.getFat());
        BigDecimal carbohydrateCalories = carbohydrateGrams.multiply(CARBOHYDRATE_CALORIES_PER_GRAM);
        BigDecimal proteinCalories = proteinGrams.multiply(PROTEIN_CALORIES_PER_GRAM);
        BigDecimal fatCalories = fatGrams.multiply(FAT_CALORIES_PER_GRAM);
        BigDecimal totalMacroCalories = carbohydrateCalories.add(proteinCalories).add(fatCalories);

        return new DailyGoalMacroRatioResponse(
                toMacroRatioMetric(carbohydrateGrams, carbohydrateCalories, totalMacroCalories, 45, 65),
                toMacroRatioMetric(proteinGrams, proteinCalories, totalMacroCalories, 10, 35),
                toMacroRatioMetric(fatGrams, fatCalories, totalMacroCalories, 20, 35)
        );
    }

    private DailyGoalMacroRatioMetricResponse toMacroRatioMetric(
            BigDecimal grams,
            BigDecimal calories,
            BigDecimal totalMacroCalories,
            int rangeMin,
            int rangeMax
    ) {
        int percent = totalMacroCalories.compareTo(BigDecimal.ZERO) == 0
                ? 0
                : calories.multiply(BigDecimal.valueOf(100))
                        .divide(totalMacroCalories, 0, RoundingMode.HALF_UP)
                        .intValue();

        return new DailyGoalMacroRatioMetricResponse(
                grams,
                calories,
                percent,
                rangeMin,
                rangeMax,
                macroStatus(percent, rangeMin, rangeMax)
        );
    }

    private String macroStatus(int percent, int rangeMin, int rangeMax) {
        if (percent < rangeMin) {
            return "LOW";
        }

        if (percent > rangeMax) {
            return "HIGH";
        }

        return "BALANCED";
    }

    private MealFood defaultNutrition(MealFood dailyNutrition) {
        if (dailyNutrition != null) {
            return dailyNutrition;
        }

        MealFood emptyNutrition = new MealFood();
        emptyNutrition.setCalories(BigDecimal.ZERO);
        emptyNutrition.setCarbohydrate(BigDecimal.ZERO);
        emptyNutrition.setProtein(BigDecimal.ZERO);
        emptyNutrition.setFat(BigDecimal.ZERO);
        return emptyNutrition;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private void validateGoalType(String goalType) {
        if (!GOAL_TYPES.contains(goalType)) {
            throw DailyGoalException.invalidGoalType();
        }
    }
}
