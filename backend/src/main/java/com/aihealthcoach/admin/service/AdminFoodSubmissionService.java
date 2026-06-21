package com.aihealthcoach.admin.service;

import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionPageResponse;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionRejectRequest;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionResponse;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionReviewRequest;

public interface AdminFoodSubmissionService {

    FoodSubmissionPageResponse findRequests(String status, int page, int size);

    FoodSubmissionResponse findRequest(Long requestId);

    FoodSubmissionResponse approveRequest(Long adminId, Long requestId, FoodSubmissionReviewRequest request);

    FoodSubmissionResponse rejectRequest(Long adminId, Long requestId, FoodSubmissionRejectRequest request);
}
