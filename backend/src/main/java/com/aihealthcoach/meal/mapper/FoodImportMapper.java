package com.aihealthcoach.meal.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aihealthcoach.meal.entity.FoodImportCandidate;
import com.aihealthcoach.meal.entity.FoodSearchMiss;

@Mapper
public interface FoodImportMapper {

    Long upsertFoodSearchMiss(
            @Param("query") String query,
            @Param("normalizedQuery") String normalizedQuery,
            @Param("userId") Long userId
    );

    FoodSearchMiss findFoodSearchMissById(@Param("searchMissId") Long searchMissId);

    FoodSearchMiss findFoodSearchMissByIdForUpdate(@Param("searchMissId") Long searchMissId);

    List<FoodSearchMiss> pickPendingFoodSearchMisses(
            @Param("limit") int limit,
            @Param("maxRetries") int maxRetries
    );

    int markFoodSearchMissProcessing(@Param("searchMissId") Long searchMissId);

    int markFoodSearchMissPendingReview(@Param("searchMissId") Long searchMissId);

    int markFoodSearchMissNoResult(@Param("searchMissId") Long searchMissId);

    int markFoodSearchMissFailed(
            @Param("searchMissId") Long searchMissId,
            @Param("failureReason") String failureReason
    );

    int markFoodSearchMissApproved(@Param("searchMissId") Long searchMissId);

    int markFoodSearchMissRejected(@Param("searchMissId") Long searchMissId);

    Long insertFoodImportRun();

    int completeFoodImportRun(
            @Param("runId") Long runId,
            @Param("status") String status,
            @Param("pickedCount") int pickedCount,
            @Param("candidateCount") int candidateCount,
            @Param("failureCount") int failureCount,
            @Param("failureReason") String failureReason
    );

    int insertFoodImportCandidate(@Param("candidate") FoodImportCandidate candidate);

    Long findFoodIdByContentHash(@Param("contentHash") String contentHash);

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
            @Param("fat") BigDecimal fat,
            @Param("carbohydrate") BigDecimal carbohydrate,
            @Param("protein") BigDecimal protein,
            @Param("contentHash") String contentHash
    );

    long countFoodSearchMissesByStatus(@Param("status") String status);

    List<FoodSearchMiss> findFoodSearchMissesByStatus(
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<FoodImportCandidate> findFoodImportCandidatesBySearchMissIds(
            @Param("searchMissIds") List<Long> searchMissIds
    );

    Long findSearchMissIdByFoodImportCandidateId(@Param("candidateId") Long candidateId);

    List<FoodImportCandidate> findFoodImportCandidatesByIdsForUpdate(
            @Param("candidateIds") List<Long> candidateIds
    );

    int approveFoodImportCandidate(
            @Param("candidateId") Long candidateId,
            @Param("adminId") Long adminId,
            @Param("foodId") Long foodId
    );

    int rejectUnselectedFoodImportCandidates(
            @Param("searchMissId") Long searchMissId,
            @Param("approvedCandidateIds") List<Long> approvedCandidateIds,
            @Param("adminId") Long adminId
    );

    int rejectFoodImportCandidatesBySearchMiss(
            @Param("searchMissId") Long searchMissId,
            @Param("adminId") Long adminId,
            @Param("rejectionReason") String rejectionReason
    );
}
