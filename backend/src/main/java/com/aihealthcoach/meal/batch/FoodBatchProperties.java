package com.aihealthcoach.meal.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "food-batch")
public record FoodBatchProperties(
        Boolean enabled,
        Integer chunkSize,
        Integer maxRetries,
        Integer maxCandidates
) {
    private static final int DEFAULT_CHUNK_SIZE = 20;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_MAX_CANDIDATES = 5;
    private static final int HARD_MAX_CANDIDATES = 5;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public int effectiveChunkSize() {
        if (chunkSize == null || chunkSize < 1) {
            return DEFAULT_CHUNK_SIZE;
        }
        return chunkSize;
    }

    public int effectiveMaxRetries() {
        if (maxRetries == null || maxRetries < 0) {
            return DEFAULT_MAX_RETRIES;
        }
        return maxRetries;
    }

    public int effectiveMaxCandidates() {
        if (maxCandidates == null || maxCandidates < 1) {
            return DEFAULT_MAX_CANDIDATES;
        }
        return Math.min(maxCandidates, HARD_MAX_CANDIDATES);
    }
}
