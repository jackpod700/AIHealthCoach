package com.aihealthcoach.meal.batch;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FoodImportBatchScheduler {

    private final FoodBatchProperties properties;
    private final FoodImportBatchRunner runner;

    @Scheduled(cron = "${food-batch.cron:0 0 * * * *}")
    public void runScheduled() {
        if (!properties.isEnabled()) {
            return;
        }

        FoodImportBatchResult result = runner.runOnce();
        log.info(
                "Food import batch finished. runId={}, status={}, picked={}, candidates={}, failures={}",
                result.runId(),
                result.status(),
                result.pickedCount(),
                result.candidateCount(),
                result.failureCount()
        );
    }
}
