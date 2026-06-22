package com.aihealthcoach.dailygoal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalRecommendationResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalUpsertRequest;
import com.aihealthcoach.dailygoal.entity.DailyGoal;
import com.aihealthcoach.dailygoal.exception.DailyGoalException;
import com.aihealthcoach.dailygoal.mapper.DailyGoalMapper;
import com.aihealthcoach.exercise.mapper.ExerciseMapper;
import com.aihealthcoach.meal.entity.MealFood;
import com.aihealthcoach.meal.mapper.MealMapper;
import com.aihealthcoach.summary.service.DailyChatSummaryStateService;
import com.aihealthcoach.user.entity.UserProfile;
import com.aihealthcoach.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyGoalServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);

    @Mock
    private DailyGoalMapper dailyGoalMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MealMapper mealMapper;

    @Mock
    private ExerciseMapper exerciseMapper;
    @Mock
    private DailyChatSummaryStateService dailyChatSummaryStateService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private DailyGoalServiceImpl dailyGoalService;

    @BeforeEach
    void setUp() {
        dailyGoalService = new DailyGoalServiceImpl(
                dailyGoalMapper,
                userMapper,
                mealMapper,
                exerciseMapper,
                dailyChatSummaryStateService,
                clock
        );
    }

    @Test
    void recommendGoalUsesRequestedGoalTypeGenderAndAge() {
        when(userMapper.findUserProfileByUserId(USER_ID)).thenReturn(UserProfile.builder()
                .heightCm(new BigDecimal("172.50"))
                .currentWeightKg(new BigDecimal("68.40"))
                .targetWeightKg(new BigDecimal("65.00"))
                .gender("MALE")
                .age(32)
                .build());

        DailyGoalRecommendationResponse response = dailyGoalService.recommendGoal(USER_ID, "MAINTENANCE");

        assertThat(response.calorieIntakeGoal()).isEqualTo(1929);
        assertThat(response.exerciseCalorieGoal()).isEqualTo(250);
    }

    @Test
    void recommendGoalRejectsMissingProfileFields() {
        when(userMapper.findUserProfileByUserId(USER_ID)).thenReturn(UserProfile.builder()
                .heightCm(new BigDecimal("172.50"))
                .currentWeightKg(new BigDecimal("68.40"))
                .build());

        assertThatThrownBy(() -> dailyGoalService.recommendGoal(USER_ID, "WEIGHT_LOSS"))
                .isInstanceOf(DailyGoalException.class)
                .extracting("errorCode")
                .isEqualTo(com.aihealthcoach.dailygoal.exception.DailyGoalErrorCode.PROFILE_REQUIRED);
    }

    @Test
    void upsertCurrentGoalSavesValuesThatFrontendWouldWarnAbout() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
        DailyGoalUpsertRequest request = new DailyGoalUpsertRequest("WEIGHT_LOSS", 1000, 1200);
        when(dailyGoalMapper.upsert(org.mockito.ArgumentMatchers.any(DailyGoal.class))).thenReturn(DailyGoal.builder()
                .userId(USER_ID)
                .goalType("WEIGHT_LOSS")
                .calorieIntakeGoal(1000)
                .exerciseCalorieGoal(1200)
                .updatedAt(updatedAt)
                .build());

        DailyGoalResponse response = dailyGoalService.upsertCurrentGoal(USER_ID, request);

        assertThat(response.updatedAt()).isEqualTo(updatedAt);
        assertThat(response.calorieIntakeGoal()).isEqualTo(1000);
        assertThat(response.exerciseCalorieGoal()).isEqualTo(1200);
        assertThat(response.goalType()).isEqualTo("WEIGHT_LOSS");
        verify(userMapper).updateUserProfileGoalType(USER_ID, "WEIGHT_LOSS");
        verify(dailyChatSummaryStateService).markChanged(USER_ID, DATE);
    }

    @Test
    void findProgressAggregatesMealAndExerciseTotals() {
        when(dailyGoalMapper.findByUserId(USER_ID)).thenReturn(DailyGoal.builder()
                .userId(USER_ID)
                .goalType("WEIGHT_LOSS")
                .calorieIntakeGoal(1600)
                .exerciseCalorieGoal(350)
                .build());
        MealFood dailyNutrition = new MealFood();
        dailyNutrition.setCalories(new BigDecimal("1234.5"));
        dailyNutrition.setCarbohydrate(new BigDecimal("180"));
        dailyNutrition.setProtein(new BigDecimal("80"));
        dailyNutrition.setFat(new BigDecimal("40"));
        when(mealMapper.sumDailyNutrition(USER_ID, DATE)).thenReturn(dailyNutrition);
        when(exerciseMapper.sumDailyCaloriesBurned(USER_ID, DATE)).thenReturn(210);

        DailyGoalProgressResponse response = dailyGoalService.findProgress(USER_ID, DATE);

        assertThat(response.progress().calorieIntake().current()).isEqualByComparingTo("1234.5");
        assertThat(response.progress().calorieIntake().remaining()).isEqualByComparingTo("365.5");
        assertThat(response.progress().calorieIntake().percent()).isEqualTo(77);
        assertThat(response.progress().exerciseCalories().current()).isEqualByComparingTo("210");
        assertThat(response.progress().exerciseCalories().remaining()).isEqualByComparingTo("140");
        assertThat(response.progress().exerciseCalories().percent()).isEqualTo(60);
        assertThat(response.macroRatio().carbohydrate().percent()).isEqualTo(51);
        assertThat(response.macroRatio().carbohydrate().status()).isEqualTo("BALANCED");
        assertThat(response.macroRatio().protein().percent()).isEqualTo(23);
        assertThat(response.macroRatio().fat().percent()).isEqualTo(26);
    }

    @Test
    void findProgressRejectsMissingGoal() {
        when(dailyGoalMapper.findByUserId(USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> dailyGoalService.findProgress(USER_ID, DATE))
                .isInstanceOf(DailyGoalException.class)
                .extracting("errorCode")
                .isEqualTo(com.aihealthcoach.dailygoal.exception.DailyGoalErrorCode.DAILY_GOAL_NOT_FOUND);
    }
}
