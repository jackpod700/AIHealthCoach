# AI Chat Single LLM Call Refactoring Plan

## Summary

Refactor `POST /api/chat/messages` so one user message triggers only one LLM call. The single AI call should produce both the user-facing assistant message and the structured meal extraction payload used to build an optional meal proposal.

The existing frontend response contract remains:

```json
{
  "messages": [],
  "mealProposal": null
}
```

The meal proposal confirmation API remains unchanged.

## Target Flow

1. The user sends a chat message.
2. The backend saves the user message.
3. A unified AI chat service calls the LLM once.
4. The LLM returns JSON containing:
   - `assistantMessage`
   - `mealExtraction`
5. The backend saves `assistantMessage` as an assistant chat message.
6. The backend builds `mealProposal` from `mealExtraction`.
7. The backend returns `{ messages, mealProposal }`.

## Internal LLM JSON Shape

Meal intent:

```json
{
  "assistantMessage": "점심 식사를 기록할 수 있도록 후보를 찾았어요.",
  "mealExtraction": {
    "mealIntent": true,
    "mealDate": "2026-06-02",
    "mealType": "LUNCH",
    "items": [
      {
        "name": "김치찌개",
        "quantity": 1
      },
      {
        "name": "밥",
        "quantity": 1
      }
    ]
  }
}
```

No meal intent:

```json
{
  "assistantMessage": "운동 루틴은 이렇게 해보세요.",
  "mealExtraction": {
    "mealIntent": false,
    "mealDate": null,
    "mealType": null,
    "items": []
  }
}
```

## Backend Changes

- Add a unified AI result DTO:
  - `AiChatResult`
  - contains `assistantMessage` and `ExtractedMealResult`
- Change `AiChatService` to return `AiChatResult` instead of saving and returning only `ChatMessageResponse`.
- Move the `ChatClient` call into `AiChatServiceImpl` as one strict JSON prompt.
- Change `AiMealProposalService`:
  - from `createProposal(String userMessage)`
  - to `createProposal(ExtractedMealResult extracted)`
- Remove the second LLM call path:
  - delete `AiMealExtractionService`
  - delete `AiMealExtractionServiceImpl`
- Update `ChatController`:
  - save user message
  - call `aiChatService.generate(userMessage)`
  - save assistant message from `AiChatResult.assistantMessage`
  - create proposal from `AiChatResult.mealExtraction`
  - return `ChatMessageSendResponse`

## TDD Steps

1. Write failing tests for `AiChatServiceImpl` JSON parsing.
2. Write failing tests for parse fallback behavior.
3. Update existing `AiMealProposalServiceImplTest` to pass `ExtractedMealResult` directly.
4. Add or update controller/service tests to prove the chat flow uses the unified AI result.
5. Implement the minimum code to pass those tests.
6. Remove obsolete extraction service classes.
7. Run backend tests.

## Test Plan

- AI JSON with meal intent returns:
  - assistant message
  - `mealIntent=true`
  - extracted items
- AI JSON without meal intent returns:
  - assistant message
  - `mealIntent=false`
- Invalid AI JSON returns:
  - fallback assistant message
  - `mealIntent=false`
- `AiMealProposalServiceImpl` no longer calls an LLM-facing service.
- `POST /api/chat/messages` still returns `{ messages, mealProposal }`.
- Existing confirm API still calls `MealService.createMeal`.

## Assumptions

- The frontend response contract does not change.
- The proposal confirmation flow does not change.
- This refactor only reduces LLM calls; it does not add unit conversion, proposal persistence, or richer candidate editing.
