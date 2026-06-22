package com.aihealthcoach.meal.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aihealthcoach.meal.dto.MealDto.MealItemRequest;
import com.aihealthcoach.meal.entity.Food;
import com.aihealthcoach.meal.entity.MealFood;
import com.aihealthcoach.meal.util.FoodSearchQuery.Token;

@Mapper
public interface MealMapper {
    List<MealFood> findDailyMeals(@Param("userId") Long userId, @Param("date") LocalDate date);

    MealFood sumDailyNutrition(@Param("userId") Long userId, @Param("date") LocalDate date);

    List<MealFood> findMealsBetween(
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

    LocalDate findMealDateById(
            @Param("userId") Long userId,
            @Param("mealId") Long mealId
    );

    int deleteMeal(@Param("userId") Long userId, @Param("mealId") Long mealId);

    void insertMealItem(@Param("mealId") Long mealId, @Param("item") MealItemRequest item);

    List<Long> findFoodIdsByIds(@Param("foodIds") List<Long> foodIds);

    long countFoodGroups(@Param("query") String query, @Param("tokens") List<Token> tokens);

    List<String> findFoodGroupSourceKeys(
            @Param("query") String query,
            @Param("tokens") List<Token> tokens,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<Food> findFoodsBySourceKeys(@Param("sourceKeys") List<String> sourceKeys);

    List<Food> searchFoods(@Param("query") String query, @Param("tokens") List<Token> tokens);

}
