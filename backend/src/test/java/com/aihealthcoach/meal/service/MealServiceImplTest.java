package com.aihealthcoach.meal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.meal.dto.MealDto.CreateMealRequest;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.meal.dto.MealDto.MealItemRequest;
import com.aihealthcoach.meal.dto.MealDto.MonthlyMealResponse;
import com.aihealthcoach.meal.entity.MealFood;
import com.aihealthcoach.meal.exception.MealErrorCode;
import com.aihealthcoach.meal.exception.MealException;
import com.aihealthcoach.meal.mapper.MealMapper;
import com.aihealthcoach.summary.entity.DailyChatSummaryChangeSource;
import com.aihealthcoach.summary.service.DailyChatSummaryStateService;

@ExtendWith(MockitoExtension.class)
class MealServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long MEAL_ID = 10L;
    private static final LocalDate MEAL_DATE = LocalDate.of(2026, 5, 30);
    private static final Long FOOD_CODE = 1234567890123456789L;

    @Mock
    private MealMapper mealMapper;
    @Mock
    private DailyChatSummaryStateService dailyChatSummaryStateService;

    @InjectMocks
    private MealServiceImpl mealService;

    @Test
    void createMealInsertsNewMealAndItems() {
        CreateMealRequest request = request("BREAKFAST", item(FOOD_CODE, "1.5"));

        when(mealMapper.findFoodIdsByIds(List.of(FOOD_CODE))).thenReturn(List.of(FOOD_CODE));
        when(mealMapper.findMealIdByUserTypeDate(USER_ID, "BREAKFAST", MEAL_DATE)).thenReturn(null);
        when(mealMapper.insertMeal(USER_ID, "BREAKFAST", MEAL_DATE)).thenReturn(MEAL_ID);
        when(mealMapper.findDailyMeals(USER_ID, MEAL_DATE)).thenReturn(List.of());

        mealService.createMeal(USER_ID, request);

        verify(mealMapper).insertMeal(USER_ID, "BREAKFAST", MEAL_DATE);
        verify(mealMapper).insertMealItem(MEAL_ID, request.items().get(0));
        verify(mealMapper, never()).deleteMealItems(any());
        verify(dailyChatSummaryStateService).markChanged(USER_ID, MEAL_DATE, DailyChatSummaryChangeSource.MEAL);
    }

    @Test
    void createMealOverwritesExistingMealItems() {
        CreateMealRequest request = request("LUNCH", item(FOOD_CODE, "2"));

        when(mealMapper.findFoodIdsByIds(List.of(FOOD_CODE))).thenReturn(List.of(FOOD_CODE));
        when(mealMapper.findMealIdByUserTypeDate(USER_ID, "LUNCH", MEAL_DATE)).thenReturn(MEAL_ID);
        when(mealMapper.findDailyMeals(USER_ID, MEAL_DATE)).thenReturn(List.of());

        mealService.createMeal(USER_ID, request);

        verify(mealMapper).deleteMealItems(MEAL_ID);
        verify(mealMapper).insertMealItem(MEAL_ID, request.items().get(0));
        verify(mealMapper, never()).insertMeal(any(), any(), any());
    }

    @Test
    void createMealRejectsInvalidMealType() {
        CreateMealRequest request = request("MIDNIGHT", item(FOOD_CODE, "1"));

        assertThatThrownBy(() -> mealService.createMeal(USER_ID, request))
                .isInstanceOf(MealException.class)
                .extracting("errorCode")
                .isEqualTo(MealErrorCode.INVALID_MEAL_TYPE);

        verify(mealMapper, never()).findMealIdByUserTypeDate(any(), any(), any());
    }

    @Test
    void createMealRejectsUnknownFood() {
        CreateMealRequest request = request("DINNER", item(FOOD_CODE, "1"));

        when(mealMapper.findFoodIdsByIds(List.of(FOOD_CODE))).thenReturn(List.of());

        assertThatThrownBy(() -> mealService.createMeal(USER_ID, request))
                .isInstanceOf(MealException.class)
                .extracting("errorCode")
                .isEqualTo(MealErrorCode.FOOD_NOT_FOUND);

        verify(mealMapper, never()).insertMeal(any(), any(), any());
    }

    @Test
    void createMealRejectsDuplicateFoodCodes() {
        CreateMealRequest request = request(
                "SNACK",
                item(FOOD_CODE, "1"),
                item(FOOD_CODE, "2")
        );

        assertThatThrownBy(() -> mealService.createMeal(USER_ID, request))
                .isInstanceOf(MealException.class)
                .extracting("errorCode")
                .isEqualTo(MealErrorCode.INVALID_MEAL_ITEM);

        verify(mealMapper, never()).insertMeal(any(), any(), any());
    }

    @Test
    void findDailyMealsIncludesServingBasisForEditing() {
        MealFood row = row(MEAL_ID, MEAL_DATE, "BREAKFAST", "1.5", "100", "20", "10", "5");
        row.setServingSize(new BigDecimal("100"));
        row.setServingUnit("g");
        when(mealMapper.findDailyMeals(USER_ID, MEAL_DATE)).thenReturn(List.of(row));

        DailyMealResponse response = mealService.findDailyMeals(USER_ID, MEAL_DATE);

        assertThat(response.meals()).hasSize(1);
        assertThat(response.meals().get(0).items()).hasSize(1);
        assertThat(response.meals().get(0).items().get(0).servingSize()).isEqualByComparingTo("100");
        assertThat(response.meals().get(0).items().get(0).servingUnit()).isEqualTo("g");
    }

    @Test
    void findMonthlyMealsGroupsDaysAndSumsNutrition() {
        LocalDate date = LocalDate.of(2026, 6, 2);
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 1);
        when(mealMapper.findMealsBetween(USER_ID, startDate, endDate)).thenReturn(List.of(
                row(10L, date, "BREAKFAST", "1.5", "100", "20", "10", "5"),
                row(10L, date, "BREAKFAST", "1", "50", "10", "5", "2"),
                row(11L, date, "DINNER", "2", "200", "30", "20", "8")
        ));

        MonthlyMealResponse response = mealService.findMonthlyMeals(USER_ID, 2026, 6);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(6);
        assertThat(response.days()).hasSize(1);
        assertThat(response.days().get(0).date()).isEqualTo(date);
        assertThat(response.days().get(0).mealCount()).isEqualTo(2);
        assertThat(response.days().get(0).mealTypes()).containsExactly("BREAKFAST", "DINNER");
        assertThat(response.days().get(0).totalCalories()).isEqualByComparingTo("600.0");
        assertThat(response.days().get(0).totalCarbohydrate()).isEqualByComparingTo("100.0");
        assertThat(response.days().get(0).totalProtein()).isEqualByComparingTo("60.0");
        assertThat(response.days().get(0).totalFat()).isEqualByComparingTo("25.5");
    }

    @Test
    void findMonthlyMealsReturnsEmptyDaysForEmptyMonth() {
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 1);
        when(mealMapper.findMealsBetween(USER_ID, startDate, endDate)).thenReturn(List.of());

        MonthlyMealResponse response = mealService.findMonthlyMeals(USER_ID, 2026, 6);

        assertThat(response.days()).isEmpty();
    }

    @Test
    void findMonthlyMealsRejectsInvalidMonth() {
        assertThatThrownBy(() -> mealService.findMonthlyMeals(USER_ID, 2026, 13))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("month must be between 1 and 12");

        verify(mealMapper, never()).findMealsBetween(any(), any(), any());
    }

    @Test
    void deleteMealDeletesOwnedMeal() {
        when(mealMapper.findMealDateById(USER_ID, MEAL_ID)).thenReturn(MEAL_DATE);
        when(mealMapper.deleteMeal(USER_ID, MEAL_ID)).thenReturn(1);

        mealService.deleteMeal(USER_ID, MEAL_ID);

        verify(mealMapper).deleteMeal(USER_ID, MEAL_ID);
        verify(dailyChatSummaryStateService).markChanged(USER_ID, MEAL_DATE, DailyChatSummaryChangeSource.MEAL);
    }

    @Test
    void deleteMealRejectsMissingOrUnownedMeal() {
        when(mealMapper.findMealDateById(USER_ID, MEAL_ID)).thenReturn(null);

        assertThatThrownBy(() -> mealService.deleteMeal(USER_ID, MEAL_ID))
                .isInstanceOf(MealException.class)
                .extracting("errorCode")
                .isEqualTo(MealErrorCode.MEAL_NOT_FOUND);

        verify(mealMapper, never()).deleteMeal(USER_ID, MEAL_ID);
    }

    private CreateMealRequest request(String mealType, MealItemRequest... items) {
        return new CreateMealRequest(MEAL_DATE, mealType, List.of(items));
    }

    private MealItemRequest item(Long foodCode, String quantity) {
        return new MealItemRequest(foodCode, new BigDecimal(quantity));
    }

    private MealFood row(
            Long mealId,
            LocalDate mealDate,
            String mealType,
            String quantity,
            String calories,
            String carbohydrate,
            String protein,
            String fat
    ) {
        MealFood row = new MealFood();
        row.setMealId(mealId);
        row.setMealDate(mealDate);
        row.setMealType(mealType);
        row.setFoodId(FOOD_CODE + mealId + Math.abs(quantity.hashCode()));
        row.setQuantity(new BigDecimal(quantity));
        row.setCalories(new BigDecimal(calories));
        row.setCarbohydrate(new BigDecimal(carbohydrate));
        row.setProtein(new BigDecimal(protein));
        row.setFat(new BigDecimal(fat));
        return row;
    }
}
