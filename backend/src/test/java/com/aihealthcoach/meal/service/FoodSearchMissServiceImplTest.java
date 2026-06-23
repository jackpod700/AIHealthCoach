package com.aihealthcoach.meal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.meal.dto.FoodSearchMissDto.FoodSearchMissCreateRequest;
import com.aihealthcoach.meal.dto.FoodSearchMissDto.FoodSearchMissResponse;
import com.aihealthcoach.meal.entity.FoodSearchMiss;
import com.aihealthcoach.meal.mapper.FoodImportMapper;

@ExtendWith(MockitoExtension.class)
class FoodSearchMissServiceImplTest {

    @Mock
    private FoodImportMapper foodImportMapper;

    @InjectMocks
    private FoodSearchMissServiceImpl foodSearchMissService;

    @Test
    void recordMissStoresValidQueryAndReturnsSavedStatus() {
        FoodSearchMiss saved = FoodSearchMiss.builder()
                .id(10L)
                .query("apple")
                .normalizedQuery("apple")
                .status("PENDING")
                .missCount(2)
                .build();

        when(foodImportMapper.upsertFoodSearchMiss("apple", "apple", 1L)).thenReturn(10L);
        when(foodImportMapper.findFoodSearchMissById(10L)).thenReturn(saved);

        FoodSearchMissResponse response = foodSearchMissService.recordMiss(
                1L,
                new FoodSearchMissCreateRequest(" apple ")
        );

        assertThat(response.accepted()).isTrue();
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.searchMissId()).isEqualTo(10L);
        assertThat(response.missCount()).isEqualTo(2);
    }

    @Test
    void recordMissIgnoresShortOrMeaninglessQuery() {
        FoodSearchMissResponse response = foodSearchMissService.recordMiss(
                1L,
                new FoodSearchMissCreateRequest("!")
        );

        assertThat(response.accepted()).isFalse();
        assertThat(response.status()).isEqualTo("IGNORED");
        verify(foodImportMapper, never()).upsertFoodSearchMiss(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void pickPendingMissesMarksSelectedRowsProcessing() {
        FoodSearchMiss first = FoodSearchMiss.builder()
                .id(10L)
                .status("PENDING")
                .build();
        FoodSearchMiss second = FoodSearchMiss.builder()
                .id(11L)
                .status("FAILED")
                .build();

        when(foodImportMapper.pickPendingFoodSearchMisses(20, 3)).thenReturn(List.of(first, second));

        List<FoodSearchMiss> result = foodSearchMissService.pickPendingMisses(20, 3);

        assertThat(result).containsExactly(first, second);
        verify(foodImportMapper).markFoodSearchMissProcessing(10L);
        verify(foodImportMapper).markFoodSearchMissProcessing(11L);
    }
}
