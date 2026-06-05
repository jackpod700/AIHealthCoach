# Meal Proposal Quantity Edit Plan

## Summary

Let users review and edit the quantity multiplier before confirming an AI-generated meal proposal.

Food candidates should show the database serving size and serving unit so the user understands what `quantity` means. The saved `meal_items.quantity` remains a multiplier of the selected food's serving basis.

## Backend Changes

- Add serving basis fields to food candidate responses:
  - `servingSize`
  - `servingUnit`
- Add the same fields to `FoodCandidateRow`.
- Update `MealMapper.xml` candidate query to select:

  ```sql
  serving_size,
  serving_unit
  ```

- Map those fields in the `foodCandidateRow` result map.
- Keep DB schema unchanged.

Example candidate response:

```json
{
  "foodCode": "1234567890123456789",
  "foodName": "김치찌개",
  "manufacturer": null,
  "servingSize": 100,
  "servingUnit": "g",
  "calories": 80,
  "carbohydrate": 5,
  "protein": 6,
  "fat": 3
}
```

## Frontend Changes

- Add proposal quantity state:
  - `mealProposalQuantities[index]`
- Initialize each quantity from `mealProposal.items[index].quantity`.
- If AI quantity is missing or invalid, default to `1`.
- Add a numeric input per proposal item.
- Send `mealProposalQuantities[index]` in the confirm API payload.
- Disable the confirm button unless:
  - every item has a selected food candidate
  - every quantity is a number greater than `0`
- Show selected candidate serving basis:
  - `기준량: 100 g`
  - `기록 수량: 기준량의 1.5배`

## UI Behavior

- The user first chooses a food candidate.
- Once selected, the UI displays that candidate's serving size and unit.
- The quantity input controls the multiplier to save.
- The first implementation does not convert grams, bowls, pieces, or milliliters.
- Changing candidates does not automatically change the multiplier.

## TDD / Test Plan

- Backend:
  - `FoodCandidateResponse.fromRow` maps `servingSize` and `servingUnit`.
  - `searchFoodCandidates` query/result map includes serving basis fields.
- Frontend:
  - No frontend test runner exists, so verify with `npm.cmd run build`.
  - Manually verify proposal display, quantity editing, disabled state, and confirm payload.

## Verification

```powershell
docker run --rm -v ${PWD}\backend:/app -v ${env:USERPROFILE}\.m2:/root/.m2 -w /app maven:3.9.11-eclipse-temurin-21 mvn test
npm.cmd run build
```

## Assumptions

- `foods.serving_size` and `foods.serving_unit` are the serving basis.
- `meal_items.quantity` remains a serving-basis multiplier.
- This change does not add unit conversion or free-form gram input.
