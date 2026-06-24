package com.aihealthcoach.meal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.meal.dto.AiMealDto.FoodCandidateResponse;
import com.aihealthcoach.meal.dto.FoodDto.FoodSearchPageResponse;
import com.aihealthcoach.meal.entity.Food;
import com.aihealthcoach.meal.mapper.MealMapper;
import com.aihealthcoach.meal.util.FoodSearchQuery.Token;

@ExtendWith(MockitoExtension.class)
class FoodServiceImplTest {

    @Mock
    private MealMapper mealMapper;

    @InjectMocks
    private FoodServiceImpl foodService;

    @Test
    void searchFoodGroupsGroupsRowsBySourceKeyWithServings() {
        List<Token> tokens = List.of(new Token("chicken", "chicken"));
        when(mealMapper.countFoodGroups("chicken", tokens)).thenReturn(1L);
        when(mealMapper.findFoodGroupSourceKeys("chicken", tokens, 20, 0)).thenReturn(List.of("source-1"));
        when(mealMapper.findFoodsBySourceKeys(List.of("source-1")))
                .thenReturn(List.of(
                        food(1L, "source-1", "Chicken Breast", "Brand", "100g", "100", "g", "120"),
                        food(2L, "source-1", "Chicken Breast", "Brand", "1 serving", "1", "serving", "180")
                ));

        FoodSearchPageResponse response = foodService.searchFoodGroups(" chicken ", 1, 20);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalItems()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).sourceKey()).isEqualTo("source-1");
        assertThat(response.items().get(0).servings()).hasSize(2);
        assertThat(response.items().get(0).servings().get(0).foodId()).isEqualTo(1L);
        assertThat(response.items().get(0).servings().get(1).servingUnit()).isEqualTo("serving");
    }

    @Test
    void searchFoodGroupsReturnsEmptyPageWhenNoSourceKeys() {
        when(mealMapper.countFoodGroups("", List.of())).thenReturn(0L);
        when(mealMapper.findFoodGroupSourceKeys("", List.of(), 20, 0)).thenReturn(List.of());

        FoodSearchPageResponse response = foodService.searchFoodGroups("", 0, 0);

        assertThat(response.items()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalItems()).isZero();
        assertThat(response.totalPages()).isZero();
        verify(mealMapper, never()).findFoodsBySourceKeys(List.of());
    }

    @Test
    void searchFoodsKeepsBlankQueryEmptyForManualSearch() {
        List<FoodCandidateResponse> response = foodService.searchFoods(" ");

        assertThat(response).isEmpty();
        verify(mealMapper, never()).countFoods("", List.of());
        verify(mealMapper, never()).searchFoods("", List.of());
    }

    @Test
    void searchFoodsReturnsLimitedCandidateItems() {
        List<Token> tokens = List.of(new Token("chicken", "chicken"));
        when(mealMapper.searchFoods("chicken", tokens))
                .thenReturn(List.of(food(1L, "source-1", "Chicken Breast", "Brand", "100g", "100", "g", "120")));

        List<FoodCandidateResponse> response = foodService.searchFoods(" chicken ");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).foodId()).isEqualTo(1L);
        verify(mealMapper, never()).countFoods("chicken", tokens);
    }

    @Test
    void countFoodsReturnsCandidateSearchCount() {
        List<Token> tokens = List.of(new Token("missing", "missing"));
        when(mealMapper.countFoods("missing", tokens)).thenReturn(42L);

        long totalItems = foodService.countFoods(" missing ");

        assertThat(totalItems).isEqualTo(42);
        verify(mealMapper, never()).searchFoods("missing", tokens);
    }

    @Test
    void countFoodsKeepsBlankQueryZero() {
        long totalItems = foodService.countFoods(" ");

        assertThat(totalItems).isZero();
        verify(mealMapper, never()).countFoods("", List.of());
    }

    private Food food(
            Long foodId,
            String sourceKey,
            String foodName,
            String brand,
            String servingDescription,
            String servingSize,
            String servingUnit,
            String calories
    ) {
        Food row = new Food();
        row.setFoodId(foodId);
        row.setSourceKey(sourceKey);
        row.setFoodName(foodName);
        row.setBrand(brand);
        row.setServingDescription(servingDescription);
        row.setServingSize(new BigDecimal(servingSize));
        row.setServingUnit(servingUnit);
        row.setCalories(new BigDecimal(calories));
        row.setCarbohydrate(BigDecimal.ZERO);
        row.setProtein(new BigDecimal("23"));
        row.setFat(new BigDecimal("2"));
        return row;
    }
}
