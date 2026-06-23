package com.aihealthcoach.meal.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretFoodItemResponse;
import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretSearchTestResponse;
import com.aihealthcoach.common.fatsecret.FatSecretFoodClient;
import com.aihealthcoach.common.fatsecret.FatSecretProperties;
import com.aihealthcoach.common.fatsecret.FatSecretTokenClient;
import com.aihealthcoach.meal.batch.FoodBatchProperties;
import com.aihealthcoach.meal.entity.FoodImportCandidate;
import com.aihealthcoach.meal.entity.FoodSearchMiss;
import com.aihealthcoach.meal.mapper.FoodImportMapper;
import com.aihealthcoach.meal.util.FatSecretFoodDescriptionParser;
import com.aihealthcoach.meal.util.FatSecretFoodDescriptionParser.ParsedFoodDescription;
import com.aihealthcoach.meal.util.FoodContentHashGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FoodImportCandidateServiceImpl implements FoodImportCandidateService {

    private static final String SOURCE_PROVIDER = "FATSECRET";
    private static final int FOODS_SOURCE_KEY_LIMIT = 40;

    private final FatSecretTokenClient tokenClient;
    private final FatSecretFoodClient foodClient;
    private final FatSecretProperties fatSecretProperties;
    private final FoodBatchProperties batchProperties;
    private final FoodImportMapper foodImportMapper;
    private final FoodSearchMissService foodSearchMissService;

    @Override
    public int importCandidates(FoodSearchMiss searchMiss, Long runId) {
        String accessToken = tokenClient.requestAccessToken();
        FatSecretSearchTestResponse response = foodClient.search(
                accessToken,
                searchMiss.getNormalizedQuery(),
                batchProperties.effectiveMaxCandidates(),
                defaultIfBlank(fatSecretProperties.region(), "KR"),
                defaultIfBlank(fatSecretProperties.language(), "ko")
        );

        Map<String, FoodImportCandidate> candidatesByHash = new LinkedHashMap<>();
        List<FatSecretFoodItemResponse> items = response.items() == null ? List.of() : response.items();
        for (FatSecretFoodItemResponse item : items) {
            if (candidatesByHash.size() >= batchProperties.effectiveMaxCandidates()) {
                break;
            }

            FoodImportCandidate candidate = toCandidate(searchMiss, runId, item);
            if (candidate != null) {
                candidatesByHash.putIfAbsent(candidate.getContentHash(), candidate);
            }
        }

        if (candidatesByHash.isEmpty()) {
            foodSearchMissService.markNoResult(searchMiss.getId());
            return 0;
        }

        int insertedCount = 0;
        for (FoodImportCandidate candidate : candidatesByHash.values()) {
            insertedCount += foodImportMapper.insertFoodImportCandidate(candidate);
        }
        foodSearchMissService.markPendingReview(searchMiss.getId());
        return insertedCount;
    }

    private FoodImportCandidate toCandidate(
            FoodSearchMiss searchMiss,
            Long runId,
            FatSecretFoodItemResponse item
    ) {
        String foodId = optional(item.foodId());
        String name = optional(item.foodName());
        if (foodId == null || name == null) {
            return null;
        }

        ParsedFoodDescription parsed = FatSecretFoodDescriptionParser.parse(item.foodDescription());
        if (!parsed.hasRequiredNutrition()) {
            return null;
        }

        String brand = optional(item.brandName());
        String contentHash = FoodContentHashGenerator.generate(
                name,
                brand,
                parsed.calories(),
                parsed.fat(),
                parsed.carbohydrate(),
                parsed.protein()
        );
        Long duplicateFoodId = foodImportMapper.findFoodIdByContentHash(contentHash);

        return FoodImportCandidate.builder()
                .searchMissId(searchMiss.getId())
                .importRunId(runId)
                .status(duplicateFoodId == null ? "PENDING" : "DUPLICATE")
                .sourceProvider(SOURCE_PROVIDER)
                .sourceKey(sourceKey(foodId))
                .sourceUrl(optional(item.foodUrl()))
                .sourceFoodType(optional(item.foodType()))
                .name(name)
                .brand(brand)
                .foodDescription(optional(item.foodDescription()))
                .servingDescription(parsed.servingDescription())
                .servingSize(parsed.servingSize())
                .servingUnit(optional(parsed.servingUnit()))
                .calories(parsed.calories())
                .fat(parsed.fat())
                .carbohydrate(parsed.carbohydrate())
                .protein(parsed.protein())
                .contentHash(contentHash)
                .duplicateFoodId(duplicateFoodId)
                .build();
    }

    private String sourceKey(String foodId) {
        String value = "fatsecret:" + foodId;
        if (value.length() <= FOODS_SOURCE_KEY_LIMIT) {
            return value;
        }

        return "fatsecret:" + sha1(foodId).substring(0, FOODS_SOURCE_KEY_LIMIT - "fatsecret:".length());
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String optional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(Objects.toString(value, "").getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 digest is not available.", exception);
        }
    }
}
