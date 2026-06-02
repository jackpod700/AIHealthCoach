# AI Meal Chat Recording Plan

## Goal

Build a flow where the user can describe a meal in natural language during chat, the AI extracts a meal recording proposal, and the user confirms the proposal before it is saved.

The feature should reduce manual food logging while avoiding silent incorrect records. The AI should assist with extraction and candidate matching, but the database write happens only after user confirmation.

## Current Context

- Chat messages are handled through `POST /api/chat/messages`.
- Meal records are stored through the existing `MealService.createMeal` flow.
- `meals` has one record per user, date, and meal type.
- `meal_items` stores the selected `foodCode` and `quantity`.
- The current meal registration policy overwrites existing items for the same date and meal type.
- There is no dedicated food search API yet, so this feature needs food candidate lookup against `foods`.

## User Flow

1. The user sends a natural language chat message, for example:

   ```text
   오늘 점심에 김치찌개랑 밥 먹었어
   ```

2. The backend stores the user message and generates the normal assistant reply.
3. The backend also asks the AI to extract a meal proposal from the message.
4. If no meal intent is detected, the response contains no proposal.
5. If a meal intent is detected, the response includes a `mealProposal`.
6. The frontend shows the proposal with food candidates.
7. The user selects one candidate per extracted food and confirms.
8. The frontend calls the confirmation API.
9. The backend saves the meal by reusing `MealService.createMeal`.
10. The backend stores an assistant confirmation message and returns the updated daily meal response.

## API Changes

### Chat Message Response

Change `POST /api/chat/messages` from returning only a message array to returning an object:

```json
{
  "messages": [
    {
      "role": "USER",
      "content": "오늘 점심에 김치찌개랑 밥 먹었어",
      "createdAt": "2026-06-02T12:30:00"
    },
    {
      "role": "ASSISTANT",
      "content": "점심 식사를 기록할 수 있도록 후보를 찾았어요. 아래에서 확인해 주세요.",
      "createdAt": "2026-06-02T12:30:02"
    }
  ],
  "mealProposal": {
    "mealDate": "2026-06-02",
    "mealType": "LUNCH",
    "items": [
      {
        "extractedName": "김치찌개",
        "quantity": 1,
        "candidates": [
          {
            "foodCode": "1234567890123456789",
            "foodName": "김치찌개",
            "manufacturer": null,
            "calories": 80,
            "carbohydrate": 5,
            "protein": 6,
            "fat": 3
          }
        ]
      }
    ],
    "defaultsApplied": ["quantity"]
  }
}
```

`GET /api/chat/messages` should keep returning the existing message array.

### Confirm Meal Proposal

Add an endpoint for saving a confirmed proposal:

```http
POST /api/chat/meal-proposals/confirm
Authorization: Bearer {token}
Content-Type: application/json
```

```json
{
  "mealDate": "2026-06-02",
  "mealType": "LUNCH",
  "items": [
    {
      "foodCode": "1234567890123456789",
      "quantity": 1
    }
  ]
}
```

Response:

```json
{
  "messages": [
    {
      "role": "ASSISTANT",
      "content": "점심 식단으로 기록했어요.",
      "createdAt": "2026-06-02T12:31:00"
    }
  ],
  "dailyMeal": {
    "date": "2026-06-02",
    "meals": [],
    "dailyTotalCalories": 0,
    "dailyTotalCarbohydrate": 0,
    "dailyTotalProtein": 0,
    "dailyTotalFat": 0
  }
}
```

## Meal Proposal Rules

- The AI extraction result is a proposal, not a saved record.
- The proposal is stored only in frontend state until the user confirms it.
- No new pending proposal table is required.
- Food candidates are found from `foods` using the extracted food name.
- Return up to 3 candidates per extracted food.
- If an extracted food has no candidates, keep the item in the proposal with an empty `candidates` list.
- The frontend must not allow confirmation until every item has a selected candidate.

## Default Values

- If the date is missing, use the server's current date.
- If meal type is missing, infer from server time:
  - `05:00-10:00`: `BREAKFAST`
  - `10:00-15:00`: `LUNCH`
  - `15:00-21:00`: `DINNER`
  - otherwise: `SNACK`
- If quantity is missing, use `1`.
- Quantity is treated as the current `meal_items.quantity` multiplier.
- Do not convert grams, milliliters, bowls, or pieces into serving-size ratios in the first implementation.

## Backend Implementation Notes

- Add an AI meal extraction service, for example `AiMealExtractionService`.
- Use `ChatClient` to ask for strict JSON output containing:
  - meal intent
  - meal date
  - meal type
  - extracted food names
  - quantity values
  - defaults applied
- Parse the AI output with `ObjectMapper`.
- If parsing fails, continue the chat response without a meal proposal.
- Add food candidate lookup against `foods.name`.
- Reuse `MealService.createMeal` in the confirmation endpoint.
- Save an assistant confirmation message after successful confirmation.

## Development Approach

Implement this feature with TDD.

1. Write failing backend tests for AI meal extraction, food candidate lookup, chat response proposal generation, and proposal confirmation.
2. Implement the minimum backend code needed to pass those tests.
3. Write failing frontend store/UI tests for receiving a `mealProposal`, selecting candidates, cancelling a proposal, and confirming a proposal.
4. Implement the minimum frontend code needed to pass those tests.
5. Refactor only after the relevant tests are green.

Do not start by wiring the full feature end-to-end. Build it in small red-green-refactor slices:

- Slice 1: AI extraction result parsing and default value handling.
- Slice 2: food candidate lookup from extracted food names.
- Slice 3: `POST /api/chat/messages` response object with optional `mealProposal`.
- Slice 4: `POST /api/chat/meal-proposals/confirm` using `MealService.createMeal`.
- Slice 5: frontend proposal state and confirmation UI.

## Frontend Implementation Notes

- Update the chat store to handle the new `POST /api/chat/messages` response object.
- Keep the returned `mealProposal` in frontend state.
- Render a proposal confirmation panel below the chat messages.
- Show date, meal type, default value indicators, extracted food names, and food candidates.
- Let the user choose one candidate per item.
- Enable the confirm button only when every item has a selected candidate.
- Cancel should clear the local proposal without calling the backend.
- Confirm should call `POST /api/chat/meal-proposals/confirm`.

## Test Plan

### Backend

- Meal intent message returns a non-null proposal.
- Non-meal chat message returns `mealProposal: null`.
- Missing date uses today's date.
- Missing meal type uses the configured time-window default.
- Missing quantity uses `1`.
- Food candidate lookup returns up to 3 candidates.
- Confirmation rejects items without valid food codes.
- Confirmation calls `MealService.createMeal`.
- Confirmation stores an assistant message.

### Frontend

- Chat response with no proposal behaves like the current chat flow.
- Chat response with a proposal displays the confirmation panel.
- Candidate selection enables the confirm button.
- Missing candidates prevent confirmation.
- Cancel clears the proposal.
- Confirm sends the selected `foodCode` and `quantity` values.

## Verification

Use the project harness when available:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check.ps1
```

If the root harness is blocked by local CRLF or Java version issues, verify backend and frontend separately with the documented project commands or Docker-based Java 21 builds.

## Follow-up Work

- Add editable date, meal type, and quantity fields in the proposal UI.
- Add a dedicated public food search API for manual correction.
- Add better unit conversion for grams, milliliters, bowls, and pieces.
- Persist pending proposals if the product needs refresh-resistant confirmation.
