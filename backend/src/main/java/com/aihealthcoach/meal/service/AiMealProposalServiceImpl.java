package com.aihealthcoach.meal.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealItem;
import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.aihealthcoach.meal.dto.AiMealDto.FoodCandidateResponse;
import com.aihealthcoach.meal.dto.AiMealDto.MealProposalItemResponse;
import com.aihealthcoach.meal.dto.AiMealDto.MealProposalResponse;
import com.aihealthcoach.meal.mapper.MealMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiMealProposalServiceImpl implements AiMealProposalService {

    private static final int CANDIDATE_LIMIT = 3;

    private final MealMapper mealMapper;
    private final Clock clock;

    @Override
    public MealProposalResponse createProposal(ExtractedMealResult extracted) {
        if (extracted == null || !extracted.mealIntent() || extracted.items() == null || extracted.items().isEmpty()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<String> defaultsApplied = new ArrayList<>();
        LocalDate mealDate = extracted.mealDate();
        String mealType = extracted.mealType();

        if (mealDate == null) {
            mealDate = now.toLocalDate();
            defaultsApplied.add("mealDate");
        }

        if (mealType == null || mealType.isBlank()) {
            mealType = inferMealType(now.toLocalTime());
            defaultsApplied.add("mealType");
        }

        List<MealProposalItemResponse> items = extracted.items().stream()
                .filter(item -> item.name() != null && !item.name().isBlank())
                .map(item -> toProposalItem(item, defaultsApplied))
                .toList();

        if (items.isEmpty()) {
            return null;
        }

        return new MealProposalResponse(mealDate, mealType, items, defaultsApplied);
    }

    private MealProposalItemResponse toProposalItem(ExtractedMealItem item, List<String> defaultsApplied) {
        BigDecimal quantity = item.quantity();

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            quantity = BigDecimal.ONE;
            if (!defaultsApplied.contains("quantity")) {
                defaultsApplied.add("quantity");
            }
        }

        List<FoodCandidateResponse> candidates = mealMapper.searchFoodCandidates(item.name(), CANDIDATE_LIMIT)
                .stream()
                .map(FoodCandidateResponse::fromRow)
                .toList();

        return new MealProposalItemResponse(item.name(), quantity, candidates);
    }

    private String inferMealType(LocalTime time) {
        if (!time.isBefore(LocalTime.of(5, 0)) && time.isBefore(LocalTime.of(10, 0))) {
            return "BREAKFAST";
        }
        if (!time.isBefore(LocalTime.of(10, 0)) && time.isBefore(LocalTime.of(15, 0))) {
            return "LUNCH";
        }
        if (!time.isBefore(LocalTime.of(15, 0)) && time.isBefore(LocalTime.of(21, 0))) {
            return "DINNER";
        }
        return "SNACK";
    }
}
