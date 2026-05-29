package com.aihealthcoach.meal.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aihealthcoach.meal.dto.MealFoodRow;

@Mapper
public interface MealMapper {
    List<MealFoodRow> findDailyMeals(@Param("userId") Long userId, @Param("date") LocalDate date);
}
