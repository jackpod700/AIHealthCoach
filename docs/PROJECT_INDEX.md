# Project Index

This document routes agents to the right project context before they change code.

## Read First

1. `AGENTS.md`
2. `PROJECT_PROFILE.md`
3. This document
4. `docs/ARCHITECTURE.md` for request-flow context
5. `docs/DOMAIN_MAP.md` for domain/table ownership when persistence is involved
6. Relevant docs for the changed area
7. Nearby implementation and tests

## Work Type Routing

| Work type | Read first | Main code paths |
|---|---|---|
| Agent/project rules | `AGENTS.md`, `PROJECT_PROFILE.md` | `AGENTS.md`, `PROJECT_PROFILE.md`, `TASK_TEMPLATE.md`, `tasks/README.md` |
| Task planning | `tasks/README.md`, `tasks/000-template.md`, `docs/PROJECT_INDEX.md` | `tasks/` |
| Backend API change | `docs/ARCHITECTURE.md`, `PROJECT_PROFILE.md`, relevant `backend/docs/*.md` | `backend/src/main/java/com/aihealthcoach/{domain}/controller/`, `backend/src/main/java/com/aihealthcoach/{domain}/service/`, `backend/src/test/` |
| DB/domain change | `docs/DOMAIN_MAP.md`, `PROJECT_PROFILE.md`, relevant domain plan in `backend/docs/` | `backend/src/main/java/com/aihealthcoach/{domain}/entity/`, `backend/src/main/java/com/aihealthcoach/{domain}/mapper/`, `backend/src/main/resources/mappers/` |
| MyBatis mapper change | `docs/DOMAIN_MAP.md`, `PROJECT_PROFILE.md`, relevant domain plan | `backend/src/main/java/com/aihealthcoach/{domain}/mapper/`, `backend/src/main/resources/mappers/*.xml` |
| DTO/request validation | `PROJECT_PROFILE.md`, relevant domain code | `backend/src/main/java/com/aihealthcoach/{domain}/dto/`, `backend/src/main/java/com/aihealthcoach/common/error/GlobalExceptionHandler.java` |
| Error response behavior | `PROJECT_PROFILE.md`, `AGENTS.md` | `backend/src/main/java/com/aihealthcoach/common/error/`, `backend/src/main/java/com/aihealthcoach/common/response/`, `backend/src/main/java/com/aihealthcoach/common/auth/` |
| Auth/JWT/logout | `backend/docs/refresh-token-redis-logout-plan.md`, `PROJECT_PROFILE.md` | `backend/src/main/java/com/aihealthcoach/user/`, `backend/src/main/java/com/aihealthcoach/common/auth/` |
| AI Chat flow | `docs/AI_CHAT/README.md`, `backend/docs/ai-chat-single-llm-call-plan.md`, `backend/docs/ai-meal-chat-recording-plan.md` | `backend/src/main/java/com/aihealthcoach/chat/`, `backend/src/main/java/com/aihealthcoach/meal/`, `backend/src/main/java/com/aihealthcoach/exercise/`, `backend/src/main/java/com/aihealthcoach/weight/` |
| LLM prompt or provider boundary | `docs/AI_CHAT/README.md`, `backend/docs/ai-chat-single-llm-call-plan.md`, `PROJECT_PROFILE.md` | `backend/src/main/java/com/aihealthcoach/chat/service/`, `backend/src/main/java/com/aihealthcoach/common/config/ChatClientConfig.java` |
| Meal feature | `backend/docs/meal-registration-api-plan.md`, `backend/docs/meal-calandar-plan.md`, `backend/docs/meal-proposal-quantity-edit-plan.md`, `backend/docs/ai-meal-chat-recording-plan.md` | `backend/src/main/java/com/aihealthcoach/meal/`, `frontend/src/views/records/`, `frontend/src/views/calendar/` |
| Exercise feature | `backend/docs/weight-based-exercise-calorie-optimization.md` | `backend/src/main/java/com/aihealthcoach/exercise/`, `data/exercise/` |
| Weight feature | `backend/docs/weight-tracking-plan.md`, `backend/docs/weight-based-exercise-calorie-optimization.md` | `backend/src/main/java/com/aihealthcoach/weight/`, `backend/src/main/java/com/aihealthcoach/exercise/` |
| Daily goal feature | `backend/docs/daily-goal-tracking-plan.md` | `backend/src/main/java/com/aihealthcoach/dailygoal/` |
| Food data/import | `backend/docs/food-csv-import-plan.md`, `data/foods/README.md` | `backend/src/main/java/com/aihealthcoach/meal/`, `data/foods/`, `data/scripts/` |
| Frontend UI/API integration | `PROJECT_PROFILE.md`, relevant backend plan | `frontend/src/api/`, `frontend/src/views/`, `frontend/src/components/`, `frontend/src/stores/`, `frontend/src/router/` |
| Tests/verification | `PROJECT_PROFILE.md`, `AGENTS.md` | `scripts/check`, `frontend/harness/scripts/`, `backend/harness/scripts/`, `backend/src/test/` |
| Data generation/benchmarks | `PROJECT_PROFILE.md`, relevant `data/*/README.md` or backend plan | `data/`, `data/exercise/`, `data/foods/`, `data/db/benchmark/` |

## Existing Planning Docs

| Topic | Document |
|---|---|
| Application architecture | `docs/ARCHITECTURE.md` |
| Domain/table ownership map | `docs/DOMAIN_MAP.md` |
| AI chat docs entry point | `docs/AI_CHAT/README.md` |
| Task rules and template | `tasks/README.md`, `tasks/000-template.md` |
| AI chat single LLM call | `backend/docs/ai-chat-single-llm-call-plan.md` |
| AI meal chat recording | `backend/docs/ai-meal-chat-recording-plan.md` |
| Meal registration API | `backend/docs/meal-registration-api-plan.md` |
| Meal calendar | `backend/docs/meal-calandar-plan.md` |
| Meal proposal quantity edit | `backend/docs/meal-proposal-quantity-edit-plan.md` |
| Food CSV import | `backend/docs/food-csv-import-plan.md` |
| Weight tracking | `backend/docs/weight-tracking-plan.md` |
| Weight-based exercise calorie optimization | `backend/docs/weight-based-exercise-calorie-optimization.md` |
| Daily goal tracking | `backend/docs/daily-goal-tracking-plan.md` |
| Refresh token, Redis, logout | `backend/docs/refresh-token-redis-logout-plan.md` |
| Meal data | `data/meals/README.md` |
| Food data | `data/foods/README.md` |

## Search Hints

- Find backend classes with `rg "class .*Controller|interface .*Service|class .*ServiceImpl|interface .*Mapper" backend/src/main/java`.
- Find mapper SQL with `rg "<select|<insert|<update|<delete" backend/src/main/resources/mappers`.
- Find frontend route/view wiring with `rg "createRouter|routes|path:" frontend/src`.
- Find API clients with `rg "fetch|axios|api" frontend/src/api frontend/src`.
- Find validation/error behavior with `rg "VALIDATION_ERROR|BusinessException|GlobalExceptionHandler|ApiResponse" backend/src/main/java`.
