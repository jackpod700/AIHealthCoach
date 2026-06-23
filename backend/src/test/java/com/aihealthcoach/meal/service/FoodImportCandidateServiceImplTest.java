package com.aihealthcoach.meal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretFoodItemResponse;
import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretSearchTestResponse;
import com.aihealthcoach.common.fatsecret.FatSecretFoodClient;
import com.aihealthcoach.common.fatsecret.FatSecretProperties;
import com.aihealthcoach.common.fatsecret.FatSecretTokenClient;
import com.aihealthcoach.meal.batch.FoodBatchProperties;
import com.aihealthcoach.meal.entity.FoodImportCandidate;
import com.aihealthcoach.meal.entity.FoodSearchMiss;
import com.aihealthcoach.meal.mapper.FoodImportMapper;

@ExtendWith(MockitoExtension.class)
class FoodImportCandidateServiceImplTest {

    @Mock
    private FatSecretTokenClient tokenClient;

    @Mock
    private FatSecretFoodClient foodClient;

    @Mock
    private FoodImportMapper foodImportMapper;

    @Mock
    private FoodSearchMissService foodSearchMissService;

    @Test
    void importCandidatesStoresParsedFatSecretCandidates() {
        FoodImportCandidateServiceImpl service = service();
        FoodSearchMiss miss = searchMiss();

        when(tokenClient.requestAccessToken()).thenReturn("token");
        when(foodClient.search("token", "apple", 5, "KR", "ko")).thenReturn(response(item("123", "Apple")));
        when(foodImportMapper.findFoodIdByContentHash(any())).thenReturn(null);
        when(foodImportMapper.insertFoodImportCandidate(any())).thenReturn(1);

        int inserted = service.importCandidates(miss, 50L);

        ArgumentCaptor<FoodImportCandidate> candidateCaptor = ArgumentCaptor.forClass(FoodImportCandidate.class);
        verify(foodImportMapper).insertFoodImportCandidate(candidateCaptor.capture());
        FoodImportCandidate candidate = candidateCaptor.getValue();
        assertThat(inserted).isEqualTo(1);
        assertThat(candidate.getSearchMissId()).isEqualTo(10L);
        assertThat(candidate.getImportRunId()).isEqualTo(50L);
        assertThat(candidate.getStatus()).isEqualTo("PENDING");
        assertThat(candidate.getSourceKey()).isEqualTo("fatsecret:123");
        assertThat(candidate.getCalories()).isEqualByComparingTo("52");
        assertThat(candidate.getCarbohydrate()).isEqualByComparingTo("13.81");
        verify(foodSearchMissService).markPendingReview(10L);
    }

    @Test
    void importCandidatesMarksExistingContentHashAsDuplicate() {
        FoodImportCandidateServiceImpl service = service();

        when(tokenClient.requestAccessToken()).thenReturn("token");
        when(foodClient.search("token", "apple", 5, "KR", "ko")).thenReturn(response(item("123", "Apple")));
        when(foodImportMapper.findFoodIdByContentHash(any())).thenReturn(99L);
        when(foodImportMapper.insertFoodImportCandidate(any())).thenReturn(1);

        service.importCandidates(searchMiss(), 50L);

        ArgumentCaptor<FoodImportCandidate> candidateCaptor = ArgumentCaptor.forClass(FoodImportCandidate.class);
        verify(foodImportMapper).insertFoodImportCandidate(candidateCaptor.capture());
        assertThat(candidateCaptor.getValue().getStatus()).isEqualTo("DUPLICATE");
        assertThat(candidateCaptor.getValue().getDuplicateFoodId()).isEqualTo(99L);
    }

    @Test
    void importCandidatesStoresAtMostFiveCandidates() {
        FoodImportCandidateServiceImpl service = service();

        when(tokenClient.requestAccessToken()).thenReturn("token");
        when(foodClient.search("token", "apple", 5, "KR", "ko"))
                .thenReturn(response(
                        item("1", "Apple 1"),
                        item("2", "Apple 2"),
                        item("3", "Apple 3"),
                        item("4", "Apple 4"),
                        item("5", "Apple 5"),
                        item("6", "Apple 6"),
                        item("7", "Apple 7")
                ));
        when(foodImportMapper.findFoodIdByContentHash(any())).thenReturn(null);
        when(foodImportMapper.insertFoodImportCandidate(any())).thenReturn(1);

        int inserted = service.importCandidates(searchMiss(), 50L);

        ArgumentCaptor<FoodImportCandidate> candidateCaptor = ArgumentCaptor.forClass(FoodImportCandidate.class);
        verify(foodImportMapper, times(5)).insertFoodImportCandidate(candidateCaptor.capture());
        assertThat(inserted).isEqualTo(5);
        assertThat(candidateCaptor.getAllValues())
                .extracting(FoodImportCandidate::getSourceKey)
                .containsExactly(
                        "fatsecret:1",
                        "fatsecret:2",
                        "fatsecret:3",
                        "fatsecret:4",
                        "fatsecret:5"
                );
    }

    @Test
    void importCandidatesMarksNoResultWhenFatSecretItemsAreInvalid() {
        FoodImportCandidateServiceImpl service = service();

        when(tokenClient.requestAccessToken()).thenReturn("token");
        when(foodClient.search("token", "apple", 5, "KR", "ko"))
                .thenReturn(response(new FatSecretFoodItemResponse("123", "Apple", "Generic", null, null, "Per 100g")));

        int inserted = service.importCandidates(searchMiss(), 50L);

        assertThat(inserted).isZero();
        verify(foodSearchMissService).markNoResult(10L);
    }

    private FoodImportCandidateServiceImpl service() {
        return new FoodImportCandidateServiceImpl(
                tokenClient,
                foodClient,
                new FatSecretProperties("id", "secret", "token-url", "search-url", "basic", "KR", "ko"),
                new FoodBatchProperties(false, 20, 3, 5),
                foodImportMapper,
                foodSearchMissService
        );
    }

    private FoodSearchMiss searchMiss() {
        return FoodSearchMiss.builder()
                .id(10L)
                .normalizedQuery("apple")
                .build();
    }

    private FatSecretSearchTestResponse response(FatSecretFoodItemResponse... items) {
        return new FatSecretSearchTestResponse(true, "apple", items.length, 0, 5, List.of(items));
    }

    private FatSecretFoodItemResponse item(String foodId, String name) {
        return new FatSecretFoodItemResponse(
                foodId,
                name,
                "Generic",
                null,
                "https://example.test/food/" + foodId,
                "Per 100g - Calories: 52kcal | Fat: 0.17g | Carbs: 13.81g | Protein: 0.26g"
        );
    }
}
