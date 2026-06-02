package com.aihealthcoach.meal.service;

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
import com.aihealthcoach.meal.dto.MealDto.MealItemRequest;
import com.aihealthcoach.meal.exception.MealErrorCode;
import com.aihealthcoach.meal.exception.MealException;
import com.aihealthcoach.meal.mapper.MealMapper;

@ExtendWith(MockitoExtension.class)
class MealServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long MEAL_ID = 10L;
    private static final LocalDate MEAL_DATE = LocalDate.of(2026, 5, 30);
    private static final String FOOD_CODE = "1234567890123456789";

    @Mock
    private MealMapper mealMapper;

    @InjectMocks
    private MealServiceImpl mealService;

    @Test
    void createMealInsertsNewMealAndItems() {
        CreateMealRequest request = request("BREAKFAST", item(FOOD_CODE, "1.5"));

        when(mealMapper.existsFoodCode(FOOD_CODE)).thenReturn(true);
        when(mealMapper.findMealIdByUserTypeDate(USER_ID, "BREAKFAST", MEAL_DATE)).thenReturn(null);
        when(mealMapper.insertMeal(USER_ID, "BREAKFAST", MEAL_DATE)).thenReturn(MEAL_ID);
        when(mealMapper.findDailyMeals(USER_ID, MEAL_DATE)).thenReturn(List.of());

        mealService.createMeal(USER_ID, request);

        verify(mealMapper).insertMeal(USER_ID, "BREAKFAST", MEAL_DATE);
        verify(mealMapper).insertMealItem(MEAL_ID, request.items().get(0));
        verify(mealMapper, never()).deleteMealItems(any());
    }

    @Test
    void createMealOverwritesExistingMealItems() {
        CreateMealRequest request = request("LUNCH", item(FOOD_CODE, "2"));

        when(mealMapper.existsFoodCode(FOOD_CODE)).thenReturn(true);
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

        when(mealMapper.existsFoodCode(FOOD_CODE)).thenReturn(false);

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

        when(mealMapper.existsFoodCode(FOOD_CODE)).thenReturn(true);

        assertThatThrownBy(() -> mealService.createMeal(USER_ID, request))
                .isInstanceOf(MealException.class)
                .extracting("errorCode")
                .isEqualTo(MealErrorCode.INVALID_MEAL_ITEM);

        verify(mealMapper, never()).insertMeal(any(), any(), any());
    }

    private CreateMealRequest request(String mealType, MealItemRequest... items) {
        return new CreateMealRequest(MEAL_DATE, mealType, List.of(items));
    }

    private MealItemRequest item(String foodCode, String quantity) {
        return new MealItemRequest(foodCode, new BigDecimal(quantity));
    }
}
