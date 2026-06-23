package com.aihealthcoach.meal.service;

import java.util.List;

import com.aihealthcoach.meal.dto.FoodSearchMissDto.FoodSearchMissCreateRequest;
import com.aihealthcoach.meal.dto.FoodSearchMissDto.FoodSearchMissResponse;
import com.aihealthcoach.meal.entity.FoodSearchMiss;

public interface FoodSearchMissService {

    FoodSearchMissResponse recordMiss(Long userId, FoodSearchMissCreateRequest request);

    List<FoodSearchMiss> pickPendingMisses(int limit, int maxRetries);

    void markPendingReview(Long searchMissId);

    void markNoResult(Long searchMissId);

    void markFailed(Long searchMissId, String failureReason);
}
