package com.aihealthcoach.meal.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.meal.entity.FoodSearchMiss;
import com.aihealthcoach.meal.mapper.FoodImportMapper;
import com.aihealthcoach.meal.service.FoodImportCandidateService;
import com.aihealthcoach.meal.service.FoodSearchMissService;

@ExtendWith(MockitoExtension.class)
class FoodImportBatchRunnerTest {

    @Mock
    private FoodSearchMissService foodSearchMissService;

    @Mock
    private FoodImportCandidateService foodImportCandidateService;

    @Mock
    private FoodImportMapper foodImportMapper;

    @Test
    void runOnceMarksMissFailedAndCompletesRunWhenCandidateImportFails() {
        FoodImportBatchRunner runner = runner();
        FoodSearchMiss miss = FoodSearchMiss.builder()
                .id(10L)
                .normalizedQuery("apple")
                .build();

        when(foodImportMapper.insertFoodImportRun()).thenReturn(90L);
        when(foodSearchMissService.pickPendingMisses(20, 3)).thenReturn(List.of(miss));
        when(foodImportCandidateService.importCandidates(miss, 90L))
                .thenThrow(new RuntimeException("FatSecret timeout"));

        FoodImportBatchResult result = runner.runOnce();

        assertThat(result.runId()).isEqualTo(90L);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.pickedCount()).isEqualTo(1);
        assertThat(result.candidateCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.failureReason()).isEqualTo("FatSecret timeout");
        verify(foodSearchMissService).markFailed(10L, "FatSecret timeout");
        verify(foodImportMapper).completeFoodImportRun(
                90L,
                "FAILED",
                1,
                0,
                1,
                "FatSecret timeout"
        );
    }

    private FoodImportBatchRunner runner() {
        return new FoodImportBatchRunner(
                new FoodBatchProperties(false, 20, 3, 5),
                foodSearchMissService,
                foodImportCandidateService,
                foodImportMapper
        );
    }
}
