package com.aihealthcoach.meal.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aihealthcoach.meal.entity.FoodSubmissionRequest;

@Mapper
public interface FoodSubmissionMapper {

    Long insertFoodSubmissionRequest(@Param("request") FoodSubmissionRequest request);

    long countFoodSubmissionRequestsByUser(@Param("userId") Long userId);

    List<FoodSubmissionRequest> findFoodSubmissionRequestsByUser(
            @Param("userId") Long userId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countFoodSubmissionRequestsByStatus(@Param("status") String status);

    List<FoodSubmissionRequest> findFoodSubmissionRequestsByStatus(
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    FoodSubmissionRequest findFoodSubmissionRequestById(@Param("requestId") Long requestId);

    FoodSubmissionRequest findFoodSubmissionRequestByIdForUpdate(@Param("requestId") Long requestId);

    Long findFoodIdBySourceKeyServing(
            @Param("sourceKey") String sourceKey,
            @Param("servingDescription") String servingDescription
    );

    Long insertApprovedFood(
            @Param("sourceKey") String sourceKey,
            @Param("sourceUrl") String sourceUrl,
            @Param("name") String name,
            @Param("brand") String brand,
            @Param("servingDescription") String servingDescription,
            @Param("servingSize") BigDecimal servingSize,
            @Param("servingUnit") String servingUnit,
            @Param("calories") BigDecimal calories,
            @Param("carbohydrate") BigDecimal carbohydrate,
            @Param("protein") BigDecimal protein,
            @Param("fat") BigDecimal fat,
            @Param("contentHash") String contentHash
    );

    int approveFoodSubmissionRequest(
            @Param("requestId") Long requestId,
            @Param("adminId") Long adminId,
            @Param("foodId") Long foodId,
            @Param("name") String name,
            @Param("brand") String brand,
            @Param("servingDescription") String servingDescription,
            @Param("servingSize") BigDecimal servingSize,
            @Param("servingUnit") String servingUnit,
            @Param("calories") BigDecimal calories,
            @Param("carbohydrate") BigDecimal carbohydrate,
            @Param("protein") BigDecimal protein,
            @Param("fat") BigDecimal fat,
            @Param("adminNote") String adminNote
    );

    int rejectFoodSubmissionRequest(
            @Param("requestId") Long requestId,
            @Param("adminId") Long adminId,
            @Param("rejectionReason") String rejectionReason
    );
}
