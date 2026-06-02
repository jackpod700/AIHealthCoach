# Meal Registration API Plan

## Goal

Add the first meal recording API to the backend. The first scope is registration only: authenticated users can save one meal for a date and meal type with one or more food items.

## Current Structure

- `meals` stores one meal per `user_id`, `meal_type`, and `meal_date`.
- `meal_items` stores foods and quantities for a meal.
- `foods` stores nutrition values used by the existing daily meal response.
- The backend already exposes `GET /api/meals/daily?date=yyyy-MM-dd`.

## API

```http
POST /api/meals
Authorization: Bearer {token}
Content-Type: application/json
```

```json
{
  "mealDate": "2026-05-30",
  "mealType": "BREAKFAST",
  "items": [
    {
      "foodCode": "1234567890123456789",
      "quantity": 1.5
    }
  ]
}
```

The response uses the existing `DailyMealResponse` shape so clients can refresh the daily meal screen immediately after saving.

## Rules

- `mealDate` is required.
- `mealType` must be one of `BREAKFAST`, `LUNCH`, `DINNER`, or `SNACK`.
- `items` must contain at least one food item.
- `foodCode` is required and must exist in `foods.code`.
- `quantity` must be greater than zero.
- Duplicate food codes in the same request are rejected.
- If the same user already has a meal for the same date and meal type, existing `meal_items` are replaced by the request items.

## Implementation

- Add `CreateMealRequest` and `MealItemRequest` records to `MealDto`.
- Add `POST /api/meals` to `MealController`.
- Add `createMeal(userId, request)` to `MealService`.
- Add mapper operations for finding or creating the meal, deleting old items, inserting new items, and checking food existence.
- Add meal-specific exceptions and wire them into the global exception handler.

## Test Plan

- Register a new meal successfully.
- Register the same date and meal type again and verify items are overwritten.
- Reject an unknown `foodCode`.
- Reject invalid `mealType`.
- Reject duplicate food codes in one request.
- Verify validation catches missing date, empty items, blank food code, and non-positive quantity.

## Follow-up Work

- Add meal update and delete APIs.
- Add food search API for selecting foods from the frontend.
- Add integration tests against PostgreSQL when the test harness supports database containers.
