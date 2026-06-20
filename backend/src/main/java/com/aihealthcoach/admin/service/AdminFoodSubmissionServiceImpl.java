package com.aihealthcoach.admin.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionPageResponse;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionRejectRequest;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionResponse;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionReviewRequest;
import com.aihealthcoach.meal.entity.FoodSubmissionRequest;
import com.aihealthcoach.meal.exception.MealException;
import com.aihealthcoach.meal.mapper.FoodSubmissionMapper;
import com.aihealthcoach.meal.service.FoodSubmissionServiceImpl;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminFoodSubmissionServiceImpl implements AdminFoodSubmissionService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final FoodSubmissionMapper foodSubmissionMapper;

    @Override
    @Transactional(readOnly = true)
    public FoodSubmissionPageResponse findRequests(String status, int page, int size) {
        String normalizedStatus = normalizeStatus(status);
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;

        long totalItems = foodSubmissionMapper.countFoodSubmissionRequestsByStatus(normalizedStatus);
        List<FoodSubmissionResponse> items = foodSubmissionMapper
                .findFoodSubmissionRequestsByStatus(normalizedStatus, normalizedSize, offset)
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

    @Override
    @Transactional(readOnly = true)
    public FoodSubmissionResponse findRequest(Long requestId) {
        return FoodSubmissionResponse.fromEntity(findRequestOrThrow(requestId));
    }

    @Override
    @Transactional
    public FoodSubmissionResponse approveRequest(Long adminId, Long requestId, FoodSubmissionReviewRequest request) {
        FoodSubmissionRequest existing = foodSubmissionMapper.findFoodSubmissionRequestByIdForUpdate(requestId);
        if (existing == null) {
            throw MealException.foodSubmissionNotFound();
        }
        if (!STATUS_PENDING.equals(existing.getStatus())) {
            throw MealException.foodSubmissionAlreadyReviewed();
        }

        String name = FoodSubmissionServiceImpl.required(request.name());
        String brand = FoodSubmissionServiceImpl.optional(request.brand());
        String servingDescription = FoodSubmissionServiceImpl.optional(request.servingDescription());
        String servingUnit = FoodSubmissionServiceImpl.optional(request.servingUnit());
        BigDecimal servingSize = request.servingSize();
        FoodSubmissionServiceImpl.validateServing(servingDescription, servingSize, servingUnit);

        String sourceKey = sourceKey(name, brand);
        Long foodId = foodSubmissionMapper.findFoodIdBySourceKeyServing(sourceKey, servingDescription);
        if (foodId == null) {
            foodId = foodSubmissionMapper.insertApprovedFood(
                    sourceKey,
                    "user-submission://food-requests/" + requestId,
                    name,
                    brand,
                    servingDescription,
                    servingSize,
                    servingUnit,
                    request.calories(),
                    request.carbohydrate(),
                    request.protein(),
                    request.fat(),
                    contentHash(name, brand, servingDescription, servingSize, servingUnit, request)
            );
        }

        int updated = foodSubmissionMapper.approveFoodSubmissionRequest(
                requestId,
                adminId,
                foodId,
                name,
                brand,
                servingDescription,
                servingSize,
                servingUnit,
                request.calories(),
                request.carbohydrate(),
                request.protein(),
                request.fat(),
                FoodSubmissionServiceImpl.optional(request.adminNote())
        );
        if (updated == 0) {
            throw MealException.foodSubmissionAlreadyReviewed();
        }

        return FoodSubmissionResponse.fromEntity(findRequestOrThrow(requestId));
    }

    @Override
    @Transactional
    public FoodSubmissionResponse rejectRequest(Long adminId, Long requestId, FoodSubmissionRejectRequest request) {
        FoodSubmissionRequest existing = foodSubmissionMapper.findFoodSubmissionRequestByIdForUpdate(requestId);
        if (existing == null) {
            throw MealException.foodSubmissionNotFound();
        }
        if (!STATUS_PENDING.equals(existing.getStatus())) {
            throw MealException.foodSubmissionAlreadyReviewed();
        }

        int updated = foodSubmissionMapper.rejectFoodSubmissionRequest(
                requestId,
                adminId,
                FoodSubmissionServiceImpl.required(request.rejectionReason())
        );
        if (updated == 0) {
            throw MealException.foodSubmissionAlreadyReviewed();
        }

        return FoodSubmissionResponse.fromEntity(findRequestOrThrow(requestId));
    }

    private FoodSubmissionRequest findRequestOrThrow(Long requestId) {
        FoodSubmissionRequest request = foodSubmissionMapper.findFoodSubmissionRequestById(requestId);
        if (request == null) {
            throw MealException.foodSubmissionNotFound();
        }
        return request;
    }

    private String normalizeStatus(String status) {
        String normalized = FoodSubmissionServiceImpl.optional(status);
        if (normalized == null) {
            return STATUS_PENDING;
        }

        String upperStatus = normalized.toUpperCase(Locale.ROOT);
        if (STATUS_PENDING.equals(upperStatus) || STATUS_APPROVED.equals(upperStatus) || STATUS_REJECTED.equals(upperStatus)) {
            return upperStatus;
        }
        throw MealException.invalidFoodSubmission();
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

    private String sourceKey(String name, String brand) {
        return digest("SHA-1", "USER_SUBMISSION|" + canonical(name) + "|" + canonical(brand));
    }

    private String contentHash(
            String name,
            String brand,
            String servingDescription,
            BigDecimal servingSize,
            String servingUnit,
            FoodSubmissionReviewRequest request
    ) {
        String raw = String.join("|",
                canonical(name),
                canonical(brand),
                canonical(servingDescription),
                decimal(servingSize),
                canonical(servingUnit),
                decimal(request.calories()),
                decimal(request.carbohydrate()),
                decimal(request.protein()),
                decimal(request.fat())
        );
        return digest("SHA-256", raw);
    }

    private String canonical(String value) {
        String normalized = FoodSubmissionServiceImpl.optional(value);
        return normalized == null ? "" : normalized.toLowerCase(Locale.ROOT);
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String digest(String algorithm, String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(algorithm + " digest is not available.", exception);
        }
    }
}
