package com.aihealthcoach.meal.exception;

import com.aihealthcoach.common.error.BusinessException;

public class MealException extends BusinessException {

    private MealException(MealErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public MealErrorCode getErrorCode() {
        return (MealErrorCode) super.getErrorCode();
    }

    public static MealException invalidMealType() {
        return new MealException(MealErrorCode.INVALID_MEAL_TYPE);
    }

    public static MealException foodNotFound() {
        return new MealException(MealErrorCode.FOOD_NOT_FOUND);
    }

    public static MealException invalidMealItem() {
        return new MealException(MealErrorCode.INVALID_MEAL_ITEM);
    }

    public static MealException mealNotFound() {
        return new MealException(MealErrorCode.MEAL_NOT_FOUND);
    }

    public static MealException invalidFoodSubmission() {
        return new MealException(MealErrorCode.INVALID_FOOD_SUBMISSION);
    }

    public static MealException foodSubmissionNotFound() {
        return new MealException(MealErrorCode.FOOD_SUBMISSION_NOT_FOUND);
    }

    public static MealException foodSubmissionAlreadyReviewed() {
        return new MealException(MealErrorCode.FOOD_SUBMISSION_ALREADY_REVIEWED);
    }

    public static MealException invalidFoodSearchMiss() {
        return new MealException(MealErrorCode.INVALID_FOOD_SEARCH_MISS);
    }

    public static MealException foodSearchMissNotFound() {
        return new MealException(MealErrorCode.FOOD_SEARCH_MISS_NOT_FOUND);
    }

    public static MealException foodImportCandidateNotFound() {
        return new MealException(MealErrorCode.FOOD_IMPORT_CANDIDATE_NOT_FOUND);
    }

    public static MealException foodImportCandidateAlreadyReviewed() {
        return new MealException(MealErrorCode.FOOD_IMPORT_CANDIDATE_ALREADY_REVIEWED);
    }

    public static MealException invalidFoodImportCandidate() {
        return new MealException(MealErrorCode.INVALID_FOOD_IMPORT_CANDIDATE);
    }
}
