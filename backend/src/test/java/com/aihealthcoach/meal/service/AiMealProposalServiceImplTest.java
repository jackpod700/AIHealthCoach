package com.aihealthcoach.meal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealItem;
import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.aihealthcoach.meal.dto.AiMealDto.MealProposalResponse;
import com.aihealthcoach.meal.entity.Food;
import com.aihealthcoach.meal.mapper.MealMapper;
import com.aihealthcoach.meal.util.FoodSearchQuery.Token;

@ExtendWith(MockitoExtension.class)
class AiMealProposalServiceImplTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private MealMapper mealMapper;

    @Mock
    private Clock clock;

    @InjectMocks
    private AiMealProposalServiceImpl aiMealProposalService;

    @Test
    void createProposalReturnsNullWhenMealIntentIsFalse() {
        assertThat(aiMealProposalService.createProposal(ExtractedMealResult.noMeal())).isNull();
    }

    @Test
    void createProposalAppliesDefaultsAndFoodCandidates() {
        when(clock.instant()).thenReturn(Instant.parse("2026-06-02T03:30:00Z"));
        when(clock.getZone()).thenReturn(SEOUL);
        when(mealMapper.searchFoodCandidates("김치찌개", List.of(new Token("김치찌개", "김치찌개")), 5))
                .thenReturn(List.of(food(1001L, "김치찌개", "80")));
        when(mealMapper.searchFoodCandidates("밥", List.of(new Token("밥", "밥")), 5))
                .thenReturn(List.of(food(1002L, "밥", "150")));
        ExtractedMealResult extracted = new ExtractedMealResult(
                true,
                null,
                null,
                List.of(
                        new ExtractedMealItem("김치찌개", null),
                        new ExtractedMealItem("밥", new BigDecimal("2"))
                )
        );

        MealProposalResponse proposal = aiMealProposalService.createProposal(extracted);

        assertThat(proposal.mealDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(proposal.mealType()).isEqualTo("LUNCH");
        assertThat(proposal.defaultsApplied()).containsExactlyInAnyOrder("mealDate", "mealType", "quantity");
        assertThat(proposal.items()).hasSize(2);
        assertThat(proposal.items().get(0).quantity()).isEqualByComparingTo("1");
        assertThat(proposal.items().get(0).candidates()).extracting("foodId").containsExactly(1001L);
        assertThat(proposal.items().get(1).quantity()).isEqualByComparingTo("2");
    }

    private Food food(Long foodId, String name, String calories) {
        Food row = new Food();
        row.setFoodId(foodId);
        row.setFoodName(name);
        row.setCalories(new BigDecimal(calories));
        row.setCarbohydrate(BigDecimal.ZERO);
        row.setProtein(BigDecimal.ZERO);
        row.setFat(BigDecimal.ZERO);
        return row;
    }
}
