package com.aihealthcoach.meal.service;

import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionCreateRequest;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionPageResponse;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionResponse;

public interface FoodSubmissionService {

    FoodSubmissionResponse createSubmission(Long userId, FoodSubmissionCreateRequest request);

    FoodSubmissionPageResponse findMySubmissions(Long userId, int page, int size);
}
