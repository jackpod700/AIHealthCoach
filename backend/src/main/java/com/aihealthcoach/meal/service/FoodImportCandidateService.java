package com.aihealthcoach.meal.service;

import com.aihealthcoach.meal.entity.FoodSearchMiss;

public interface FoodImportCandidateService {

    int importCandidates(FoodSearchMiss searchMiss, Long runId);
}
