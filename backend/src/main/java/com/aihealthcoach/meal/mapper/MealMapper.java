package com.aihealthcoach.meal.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aihealthcoach.meal.dto.MealDto.MealItemRequest;
import com.aihealthcoach.meal.dto.FoodCandidateRow;
import com.aihealthcoach.meal.dto.MealFoodRow;

@Mapper
public interface MealMapper {
    List<MealFoodRow> findDailyMeals(@Param("userId") Long userId, @Param("date") LocalDate date);

    List<MealFoodRow> findMealsBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Long findMealIdByUserTypeDate(
            @Param("userId") Long userId,
            @Param("mealType") String mealType,
            @Param("mealDate") LocalDate mealDate
    );

    Long insertMeal(
            @Param("userId") Long userId,
            @Param("mealType") String mealType,
            @Param("mealDate") LocalDate mealDate
    );

    void deleteMealItems(@Param("mealId") Long mealId);

    void insertMealItem(@Param("mealId") Long mealId, @Param("item") MealItemRequest item);

    boolean existsFoodCode(@Param("foodCode") String foodCode);

    List<FoodCandidateRow> searchFoodCandidates(@Param("query") String query, @Param("limit") int limit);
}
