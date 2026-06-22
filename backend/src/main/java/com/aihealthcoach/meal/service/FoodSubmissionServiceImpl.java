package com.aihealthcoach.meal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionCreateRequest;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionPageResponse;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionResponse;
import com.aihealthcoach.meal.entity.FoodSubmissionRequest;
import com.aihealthcoach.meal.exception.MealException;
import com.aihealthcoach.meal.mapper.FoodSubmissionMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FoodSubmissionServiceImpl implements FoodSubmissionService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final FoodSubmissionMapper foodSubmissionMapper;

    @Override
    @Transactional
    public FoodSubmissionResponse createSubmission(Long userId, FoodSubmissionCreateRequest request) {
        validateServing(request.servingDescription(), request.servingSize(), request.servingUnit());

        FoodSubmissionRequest submission = FoodSubmissionRequest.builder()
                .submittedByUserId(userId)
                .name(required(request.name()))
                .brand(optional(request.brand()))
                .servingDescription(optional(request.servingDescription()))
                .servingSize(request.servingSize())
                .servingUnit(optional(request.servingUnit()))
                .calories(request.calories())
                .carbohydrate(request.carbohydrate())
                .protein(request.protein())
                .fat(request.fat())
                .build();

        Long requestId = foodSubmissionMapper.insertFoodSubmissionRequest(submission);
        return FoodSubmissionResponse.fromEntity(findRequestOrThrow(requestId));
    }

    @Override
    @Transactional(readOnly = true)
    public FoodSubmissionPageResponse findMySubmissions(Long userId, int page, int size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;

        long totalItems = foodSubmissionMapper.countFoodSubmissionRequestsByUser(userId);
        List<FoodSubmissionResponse> items = foodSubmissionMapper
                .findFoodSubmissionRequestsByUser(userId, normalizedSize, offset)
                .stream()
                .map(FoodSubmissionResponse::fromEntity)
                .toList();

        return new FoodSubmissionPageResponse(
                items,
                normalizedPage,
                normalizedSize,
                totalItems,
                totalPages(totalItems, normalizedSize)
        );
    }

    private FoodSubmissionRequest findRequestOrThrow(Long requestId) {
        FoodSubmissionRequest request = foodSubmissionMapper.findFoodSubmissionRequestById(requestId);
        if (request == null) {
            throw MealException.foodSubmissionNotFound();
        }
        return request;
    }

    public static void validateServing(String servingDescription, java.math.BigDecimal servingSize, String servingUnit) {
        boolean hasDescription = optional(servingDescription) != null;
        boolean hasStructuredServing = servingSize != null && optional(servingUnit) != null;

        if (!hasDescription && !hasStructuredServing) {
            throw MealException.invalidFoodSubmission();
        }
    }

    public static String required(String value) {
        String normalized = optional(value);
        if (normalized == null) {
            throw MealException.invalidFoodSubmission();
        }
        return normalized;
    }

    public static String optional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizePage(int page) {
        return page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private int totalPages(long totalItems, int size) {
        if (totalItems == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalItems / size);
    }
}
