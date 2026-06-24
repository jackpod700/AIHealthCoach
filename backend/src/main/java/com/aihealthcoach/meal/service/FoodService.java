package com.aihealthcoach.meal.service;

import java.util.List;

import com.aihealthcoach.meal.dto.AiMealDto.FoodCandidateResponse;
import com.aihealthcoach.meal.dto.FoodDto.FoodSearchPageResponse;

public interface FoodService {

    FoodSearchPageResponse searchFoodGroups(String query, int page, int size);

    List<FoodCandidateResponse> searchFoods(String query);

    long countFoods(String query);
}
