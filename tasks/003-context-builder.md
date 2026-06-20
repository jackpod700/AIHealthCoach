# 003 Context Builder

## Status

done

## Goal

AI Chat에서 LLM을 호출하기 전에 사용자 컨텍스트를 조립하는 `ContextBuilder`를 추가한다.

`ContextBuilder`는 사용자 프로필, 현재 일일 목표, 당일 식사/운동, 최근 대화를 `UserChatContext`로 만들고 `LlmRequest`에 전달한다.

AI Chat은 context 조립과 LLM 호출을 먼저 수행한 뒤 사용자 메시지와 assistant 메시지를 저장한다. context 조립 실패 또는 LLM 실패 시에도 fallback assistant 메시지와 함께 사용자 발화가 저장되어야 한다.

상위 흐름과 확장 방향은 `backend/docs/ai-chat-context-builder-plan.md`를 따른다.

## Read First

1. `AGENTS.md`
2. `PROJECT_PROFILE.md`
3. `docs/PROJECT_INDEX.md`
4. `docs/AI_CHAT/README.md`
5. `backend/docs/ai-chat-context-builder-plan.md`
6. `tasks/001-llm-service-interface.md`
7. `tasks/002-fake-llm-harness.md`
8. Current AI Chat code:
   - `backend/src/main/java/com/aihealthcoach/chat/controller/ChatController.java`
   - `backend/src/main/java/com/aihealthcoach/chat/service/AiChatService.java`
   - `backend/src/main/java/com/aihealthcoach/chat/service/AiChatServiceImpl.java`
   - `backend/src/main/java/com/aihealthcoach/chat/service/ChatService.java`
   - `backend/src/main/java/com/aihealthcoach/chat/service/LlmService.java`
   - `backend/src/main/java/com/aihealthcoach/chat/dto/ChatDto.java`
   - `backend/src/main/java/com/aihealthcoach/chat/dto/LlmDto.java`
9. Context source services:
   - `backend/src/main/java/com/aihealthcoach/user/service/UserService.java`
   - `backend/src/main/java/com/aihealthcoach/dailygoal/service/DailyGoalService.java`
   - `backend/src/main/java/com/aihealthcoach/meal/service/MealService.java`
   - `backend/src/main/java/com/aihealthcoach/exercise/service/ExerciseService.java`
   - `backend/src/main/java/com/aihealthcoach/weight/service/WeightRecordService.java`

## Detailed Design

### Context DTO

`chat/dto`에 `ChatContextDto`를 추가하고 다음 nested record를 둔다.

- `UserChatContext`
  - `UserProfileResponse profile`
  - `DailyGoalResponse dailyGoal`
  - `DailyMealResponse dailyMeals`
  - `List<ExerciseRecordResponse> dailyExercises`
  - `List<ChatMessageResponse> recentTurns`

`DailyGoalProgressResponse`는 context에 넣지 않는다. 당일 meal/exercise 데이터가 이미 있으므로 progress를 중복 수집하지 않는다.

### Service Contracts

`chat/service`에 다음 contract를 추가한다.

```java
UserChatContext build(Long userId, LocalDate contextDate);
```

`AiChatService`는 authenticated `userId`를 받도록 변경한다.

```java
AiChatResult generate(Long userId, ChatMessageRequest userMessage);

AiChatResult generateWithImages(Long userId, String content, List<MultipartFile> images);
```

`LlmDto.LlmRequest`는 `UserChatContext context` field를 추가한다.

### Read Contracts

기존 API용 service 계약의 의미를 변경하지 않는다. context의 "없어도 정상" 요구에 맞는 새 read method를 추가한다.

- `UserService.findProfileIfExists(Long userId)`
  - profile이 없으면 `null`
- `DailyGoalService.findCurrentGoalIfExists(Long userId)`
  - goal이 없으면 `null`
  - `findProgress(...)`는 사용하지 않는다.
- `ChatService.findRecentMessages(Long userId, int limit)`
  - 최신순 query 후 오래된 순으로 반환
  - `ORDER BY created_at DESC, id DESC LIMIT #{limit}`를 사용하고 service에서 순서를 뒤집는다.
- `MealService.findDailyMeals(...)`와 `ExerciseService.findExerciseRecordsByDate(...)`는 기존 method를 재사용한다.

### Context Rules

- 기준일은 `AiChatServiceImpl`의 injected `Clock`으로 결정한다.
- 현재 체중은 `profile.currentWeightKg`를 사용하며, 별도 체중 기록 조회는 하지 않는다.
- 최근 대화 limit은 `10`개 메시지로 고정한다. 이는 최대 5왕복에 해당한다.
- context 조립은 사용자 메시지가 DB에 저장되기 전에 실행하므로 현재 입력을 recent turns에서 별도로 제외할 필요가 없다.
- profile, goal, meal, exercise, history가 없으면 해당 context section은 `null` 또는 empty value가 된다.

### Failure And Persistence Policy

- profile/goal/history의 단순 부재는 context builder 예외가 아니다.
- 예상하지 못한 context source 조회 실패는 `AiChatServiceImpl`의 기존 fallback 경로로 처리한다.
- `AiChatServiceImpl`은 context 조립과 LLM 호출을 같은 fallback 범위에서 처리하고 fallback `AiChatResult`를 반환한다.
- `ChatController`는 `AiChatService` 반환 뒤 사용자 메시지와 assistant 메시지를 항상 저장한다.
- 따라서 context builder 또는 LLM 호출 실패가 사용자 발화 유실로 이어지면 안 된다.

## Scope

- `chat/service`에 `ContextBuilder`와 구현체를 추가한다.
- `chat/dto`에 `ChatContextDto.UserChatContext`와 nested context record를 추가한다.
- `LlmDto.LlmRequest`가 `UserChatContext`를 전달하도록 확장한다.
- `AiChatService`와 `AiChatServiceImpl`이 `userId`를 받고 context를 조립하도록 수정한다.
- `ChatController`의 AI 호출과 메시지 저장 순서를 context build/LLM 호출 후 저장으로 변경한다.
- profile, daily goal, 당일 meal/exercise, recent turns을 조립하기 위한 context read method를 추가한다.
- `AiChatServiceImplTest`, `ContextBuilder` 단위 테스트, Fake LLM harness context 전달 테스트를 추가 또는 수정한다.

## Do Not Implement

- user memory 또는 daily summary 저장/조회
- PromptBuilder 분리 또는 context의 자연어 prompt 렌더링
- LLM provider 교체, dependency 추가, call logging
- 프론트엔드 변경
- DB schema 또는 migration 변경
- 미래 날짜 몸무게 기록 차단
- 과거 전체 기록/대화 이력 전달
- 사용자 기록 생성/수정 규칙 변경

## Related Tables

- `user_profiles`
- `daily_goals`
- `meals`
- `meal_foods`
- `exercise_records`
- `chat_messages`

## Invariants

- `ContextBuilder`는 읽기 전용이다.
- `userId`는 인증된 사용자 컨텍스트에서만 전달된다.
- context 기준일은 injected `Clock`으로 결정한다.
- 최근 대화는 최대 10개이며 시간순으로 context에 포함한다.
- profile, goal, records, history가 없는 경우에도 AI Chat 호출은 fallback 또는 정상 응답으로 끝난다.
- chat 계층은 다른 도메인의 controller 또는 mapper를 직접 호출하지 않는다.
- context/LLM 오류 뒤에도 USER와 ASSISTANT 메시지는 모두 저장된다.
- 기존 Chat API 응답 형식은 변경하지 않는다.
- 테스트는 실제 LLM provider 또는 `ChatClient`를 호출하지 않는다.

## Acceptance Criteria

- [x] `ContextBuilder.build(userId, contextDate)`가 `UserChatContext`를 만든다.
- [x] `UserChatContext`가 profile, current goal, daily meals, daily exercises, recent turns를 표현한다.
- [x] context 전용 optional read contract가 기존 API용 service contract를 바꾸지 않는다.
- [x] `LlmRequest`가 `UserChatContext`를 보관한다.
- [x] `AiChatServiceImpl`이 context를 만든 뒤 `LlmService`를 호출한다.
- [x] `ChatController`는 AI 호출 후 USER/ASSISTANT 메시지를 저장한다.
- [x] 빈 profile, goal, records, history가 AI Chat 실패로 이어지지 않는다.
- [x] 최근 대화는 최대 10개, 시간순이며 현재 입력을 중복하지 않는다.
- [x] context/LLM 실패에도 사용자 발화와 fallback assistant 메시지가 저장된다.
- [x] Fake LLM harness에서 `LlmRequest.context`를 검증할 수 있다.
- [x] 실제 provider API key 없이 관련 테스트가 통과한다.

## Verification

```bash
./scripts/check
```

WSL 환경에서 root harness가 막히면:

```bash
./scripts/check-wsl
```

전체 검증이 어렵다면:

```bash
cd backend && mvn test
```

## Tests

- 추가:
  - `ContextBuilder`가 각 source를 조립하는 단위 테스트
  - profile/goal/records/history가 없는 context 테스트
  - recent turns 10개 제한과 시간순 정렬 테스트
  - context/LLM failure 뒤 USER/ASSISTANT 저장 테스트
  - Fake LLM harness의 `LlmRequest.context` 검증 테스트
- 수정:
  - `AiChatServiceImplTest`
  - `AiChatHarness`
  - `ChatController` 관련 테스트가 있다면 메시지 저장 순서 테스트
- 제외:
  - 모든 source를 포함하는 controller-to-DB end-to-end 테스트
  - memory/summary 시나리오
