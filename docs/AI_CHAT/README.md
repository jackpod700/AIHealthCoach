# AI Chat Docs

This directory is the entry point for AI Chat-related work. Use it to find the right AI Chat docs, task files, and code paths before editing.

Common engineering rules live in `AGENTS.md`. Project-wide paths, commands, and conventions live in `PROJECT_PROFILE.md`. Task creation rules live in `tasks/README.md`.

## Read For AI Chat Work

For non-trivial AI Chat work, start with:

1. `docs/ARCHITECTURE.md`
2. `docs/DOMAIN_MAP.md`
3. The relevant task file under `tasks/`

## Current Implementation

- `ChatController` owns the `/api/chat` HTTP boundary.
- `ChatService` stores and reads chat messages through `ChatMapper`.
- `AiChatService` calls the current AI boundary and returns `AiChatResult`.
- `AiChatServiceImpl` contains the current AI chat implementation.
- `AiPromptFactory` builds the current text and image prompts.
- `ChatClient` is the current provider-facing Spring AI client.
- Meal, exercise, and weight extraction results are converted into proposal responses before returning to the frontend.
- Meal proposal confirmation writes through the meal domain.

## AI Chat Principles

- One user chat message should produce one primary AI chat result, not separate LLM calls per proposal type.
- AI extraction results are proposal sources, not trusted database writes.
- User confirmation is required before meal proposals become meal records.
- Prompt and parsing behavior should be testable without network access.
- Existing meal, exercise, and weight domain services should be reused instead of bypassed.

## Target Direction

These names describe target architecture. Do not assume they already exist in code.

| Area | Target meaning | Related task |
|---|---|---|
| `LlmService` | Hide provider-specific clients such as `ChatClient`; allow production and fake/test implementations. | `tasks/001-llm-service-interface.md` |
| `FakeLlmService` / harness | Make AI Chat behavior deterministic without real provider calls. | planned |
| `ContextBuilder` | Collect profile, daily goals, today's records, latest weight, recent chat turns, memories, and summaries before prompting. | planned |
| `PromptBuilder` | Separate stable prompt instructions from dynamic user context. | planned |
| User memory | Store explicit long-term user preferences or constraints and include active memories in context. | planned |
| Daily summaries | Reduce prompt length while keeping original records as the source of truth. | planned |
| LLM call logs | Track purpose, model, token usage, latency, success, and errors. | planned |

## Main Code Paths

### Backend Chat

- Chat HTTP boundary: `backend/src/main/java/com/aihealthcoach/chat/controller/ChatController.java`
- Chat service: `backend/src/main/java/com/aihealthcoach/chat/service/ChatService.java`
- AI chat service: `backend/src/main/java/com/aihealthcoach/chat/service/AiChatService.java`
- AI implementation: `backend/src/main/java/com/aihealthcoach/chat/service/AiChatServiceImpl.java`
- Prompt factory: `backend/src/main/java/com/aihealthcoach/chat/service/AiPromptFactory.java`
- Chat DTOs: `backend/src/main/java/com/aihealthcoach/chat/dto/ChatDto.java`
- Chat persistence: `backend/src/main/java/com/aihealthcoach/chat/mapper/ChatMapper.java`
- Chat mapper XML: `backend/src/main/resources/mappers/ChatMapper.xml`
- Meal proposal confirmation: `backend/src/main/java/com/aihealthcoach/chat/service/ChatMealProposalService.java`

### Frontend Chat

- Chat view: `frontend/src/views/chat/`
- Chat components: `frontend/src/components/chat/`
- Chat store: `frontend/src/stores/chatStore.js`
