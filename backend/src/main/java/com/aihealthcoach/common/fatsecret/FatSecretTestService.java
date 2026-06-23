package com.aihealthcoach.common.fatsecret;

import org.springframework.stereotype.Service;

import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretSearchTestResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FatSecretTestService {

    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final int MAX_RESULTS_LIMIT = 10;

    private final FatSecretTokenClient tokenClient;
    private final FatSecretFoodClient foodClient;
    private final FatSecretProperties properties;

    public FatSecretSearchTestResponse search(String query, Integer maxResults, String region, String language) {
        String normalizedQuery = normalizeQuery(query);
        int normalizedMaxResults = normalizeMaxResults(maxResults);
        String normalizedRegion = defaultIfBlank(region, defaultIfBlank(properties.region(), "KR"));
        String normalizedLanguage = defaultIfBlank(language, defaultIfBlank(properties.language(), "ko"));

        String accessToken = tokenClient.requestAccessToken();
        return foodClient.search(
                accessToken,
                normalizedQuery,
                normalizedMaxResults,
                normalizedRegion,
                normalizedLanguage
        );
    }

    private String normalizeQuery(String query) {
        String normalized = defaultIfBlank(query, "apple").trim();
        if (normalized.isBlank()) {
            return "apple";
        }

        return normalized;
    }

    private int normalizeMaxResults(Integer maxResults) {
        if (maxResults == null || maxResults < 1) {
            return DEFAULT_MAX_RESULTS;
        }

        return Math.min(maxResults, MAX_RESULTS_LIMIT);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
