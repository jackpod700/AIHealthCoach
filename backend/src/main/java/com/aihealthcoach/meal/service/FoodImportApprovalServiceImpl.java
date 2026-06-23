package com.aihealthcoach.meal.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateGroupResponse;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidatePageResponse;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateResponse;
import com.aihealthcoach.meal.entity.FoodImportCandidate;
import com.aihealthcoach.meal.entity.FoodSearchMiss;
import com.aihealthcoach.meal.exception.MealException;
import com.aihealthcoach.meal.mapper.FoodImportMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FoodImportApprovalServiceImpl implements FoodImportApprovalService {

    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_DUPLICATE = "DUPLICATE";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> SEARCH_MISS_STATUSES = Set.of(
            "PENDING",
            "PROCESSING",
            "PENDING_REVIEW",
            "APPROVED",
            "REJECTED",
            "NO_RESULT",
            "FAILED"
    );

    private final FoodImportMapper foodImportMapper;

    @Override
    @Transactional(readOnly = true)
    public FoodImportCandidatePageResponse findCandidateGroups(String status, int page, int size) {
        String normalizedStatus = normalizeSearchMissStatus(status);
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;

        long totalItems = foodImportMapper.countFoodSearchMissesByStatus(normalizedStatus);
        List<FoodSearchMiss> searchMisses = foodImportMapper.findFoodSearchMissesByStatus(
                normalizedStatus,
                normalizedSize,
                offset
        );

        if (searchMisses.isEmpty()) {
            return new FoodImportCandidatePageResponse(List.of(), normalizedPage, normalizedSize, totalItems, totalPages(totalItems, normalizedSize));
        }

        List<Long> searchMissIds = searchMisses.stream()
                .map(FoodSearchMiss::getId)
                .toList();
        Map<Long, List<FoodImportCandidate>> candidatesBySearchMissId = foodImportMapper
                .findFoodImportCandidatesBySearchMissIds(searchMissIds)
                .stream()
                .collect(Collectors.groupingBy(
                        FoodImportCandidate::getSearchMissId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<FoodImportCandidateGroupResponse> items = searchMisses.stream()
                .map(searchMiss -> FoodImportCandidateGroupResponse.of(
                        searchMiss,
                        candidatesBySearchMissId.getOrDefault(searchMiss.getId(), List.of())
                ))
                .toList();

        return new FoodImportCandidatePageResponse(
                items,
                normalizedPage,
                normalizedSize,
                totalItems,
                totalPages(totalItems, normalizedSize)
        );
    }

    @Override
    @Transactional
    public FoodImportCandidateResponse approveCandidate(Long adminId, Long candidateId) {
        if (candidateId == null) {
            throw MealException.invalidFoodImportCandidate();
        }
        Long searchMissId = foodImportMapper.findSearchMissIdByFoodImportCandidateId(candidateId);
        if (searchMissId == null) {
            throw MealException.foodImportCandidateNotFound();
        }

        FoodImportCandidateGroupResponse group = approveCandidatesForSearchMiss(
                adminId,
                searchMissId,
                List.of(candidateId)
        );
        return group.candidates().stream()
                .filter(item -> candidateId.equals(item.candidateId()))
                .findFirst()
                .orElseThrow(MealException::invalidFoodImportCandidate);
    }

    @Override
    @Transactional
    public FoodImportCandidateGroupResponse approveCandidates(Long adminId, Long searchMissId, List<Long> candidateIds) {
        return approveCandidatesForSearchMiss(adminId, searchMissId, candidateIds);
    }

    private FoodImportCandidateGroupResponse approveCandidatesForSearchMiss(
            Long adminId,
            Long searchMissId,
            List<Long> candidateIds
    ) {
        List<Long> normalizedCandidateIds = normalizeCandidateIds(candidateIds);

        FoodSearchMiss searchMiss = foodImportMapper.findFoodSearchMissByIdForUpdate(searchMissId);
        if (searchMiss == null) {
            throw MealException.foodSearchMissNotFound();
        }
        if (!STATUS_PENDING_REVIEW.equals(searchMiss.getStatus())) {
            throw MealException.foodImportCandidateAlreadyReviewed();
        }

        List<FoodImportCandidate> candidates = foodImportMapper.findFoodImportCandidatesByIdsForUpdate(normalizedCandidateIds);
        if (candidates.size() != normalizedCandidateIds.size()) {
            throw MealException.foodImportCandidateNotFound();
        }

        for (FoodImportCandidate candidate : candidates) {
            if (!searchMissId.equals(candidate.getSearchMissId())) {
                throw MealException.invalidFoodImportCandidate();
            }
            if (!isApprovable(candidate)) {
                throw MealException.foodImportCandidateAlreadyReviewed();
            }
        }

        for (FoodImportCandidate candidate : candidates) {
            Long foodId = resolveFoodId(candidate);
            int updated = foodImportMapper.approveFoodImportCandidate(candidate.getId(), adminId, foodId);
            if (updated == 0) {
                throw MealException.foodImportCandidateAlreadyReviewed();
            }
        }

        foodImportMapper.rejectUnselectedFoodImportCandidates(searchMissId, normalizedCandidateIds, adminId);
        foodImportMapper.markFoodSearchMissApproved(searchMissId);

        FoodSearchMiss approved = foodImportMapper.findFoodSearchMissById(searchMissId);
        List<FoodImportCandidate> reviewedCandidates = foodImportMapper.findFoodImportCandidatesBySearchMissIds(List.of(searchMissId));
        return FoodImportCandidateGroupResponse.of(approved, reviewedCandidates);
    }

    @Override
    @Transactional
    public FoodImportCandidateGroupResponse rejectSearchMiss(Long adminId, Long searchMissId, String rejectionReason) {
        FoodSearchMiss searchMiss = foodImportMapper.findFoodSearchMissByIdForUpdate(searchMissId);
        if (searchMiss == null) {
            throw MealException.foodSearchMissNotFound();
        }
        if (!STATUS_PENDING_REVIEW.equals(searchMiss.getStatus())) {
            throw MealException.foodImportCandidateAlreadyReviewed();
        }

        foodImportMapper.rejectFoodImportCandidatesBySearchMiss(
                searchMissId,
                adminId,
                defaultIfBlank(rejectionReason, "Rejected by admin.")
        );
        foodImportMapper.markFoodSearchMissRejected(searchMissId);

        FoodSearchMiss rejected = foodImportMapper.findFoodSearchMissById(searchMissId);
        List<FoodImportCandidate> candidates = foodImportMapper.findFoodImportCandidatesBySearchMissIds(List.of(searchMissId));
        return FoodImportCandidateGroupResponse.of(rejected, candidates);
    }

    private Long resolveFoodId(FoodImportCandidate candidate) {
        Long foodId = candidate.getDuplicateFoodId();
        if (foodId == null) {
            foodId = foodImportMapper.findFoodIdByContentHash(candidate.getContentHash());
        }
        if (foodId != null) {
            return foodId;
        }

        foodId = foodImportMapper.findFoodIdBySourceKeyServing(candidate.getSourceKey(), candidate.getServingDescription());
        if (foodId != null) {
            return foodId;
        }

        return foodImportMapper.insertApprovedFood(
                candidate.getSourceKey(),
                defaultIfBlank(candidate.getSourceUrl(), "fatsecret://foods/" + candidate.getSourceKey()),
                candidate.getName(),
                candidate.getBrand(),
                candidate.getServingDescription(),
                candidate.getServingSize(),
                candidate.getServingUnit(),
                candidate.getCalories(),
                candidate.getFat(),
                candidate.getCarbohydrate(),
                candidate.getProtein(),
                candidate.getContentHash()
        );
    }

    private boolean isApprovable(FoodImportCandidate candidate) {
        return STATUS_PENDING.equals(candidate.getStatus()) || STATUS_DUPLICATE.equals(candidate.getStatus());
    }

    private List<Long> normalizeCandidateIds(List<Long> candidateIds) {
        if (candidateIds == null) {
            throw MealException.invalidFoodImportCandidate();
        }

        List<Long> normalized = candidateIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw MealException.invalidFoodImportCandidate();
        }
        return normalized;
    }

    private String normalizeSearchMissStatus(String status) {
        String normalized = defaultIfBlank(status, STATUS_PENDING_REVIEW).toUpperCase(Locale.ROOT);
        if (!SEARCH_MISS_STATUSES.contains(normalized)) {
            throw MealException.invalidFoodImportCandidate();
        }
        return normalized;
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

    private String defaultIfBlank(String value, String defaultValue) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? defaultValue : normalized;
    }
}
