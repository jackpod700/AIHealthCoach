# 006 User Memory Chat Routing

## Status

done

## Goal

AI Chat에서 사용자가 "기억해줘", "내 정보에 추가해줘"처럼 명시적으로 memory 저장을 요청한 경우를 인식하고, 005의 `UserMemoryService`를 통해 해당 명령을 처리한다.

첫 버전은 **명시적 명령**만 처리한다. 일반 대화에서 선호나 제약을 추론해 자동 저장하지 않는다.

## Read First

1. `AGENTS.md`
2. `PROJECT_PROFILE.md`
3. `docs/AI_CHAT/README.md`
4. `tasks/005-user-memory.md`
5. `tasks/002-fake-llm-harness.md`
6. `backend/src/main/java/com/aihealthcoach/chat/service/AiPromptFactory.java`
7. `backend/src/main/java/com/aihealthcoach/chat/service/AiChatServiceImpl.java`
8. `backend/src/main/java/com/aihealthcoach/chat/dto/ChatDto.java`

## Target Behavior

- "유제품을 피하고 싶은 걸 기억해줘" 같은 명시적 저장 요청을 AI Chat이 memory command로 인식한다.
- LLM 결과는 assistant 응답과 memory command extraction을 함께 반환한다.
- `AiChatService`는 005의 `UserMemoryService`를 직접 호출해 memory를 저장한다. 서버 내부에서 HTTP API를 호출하지 않는다.
- 저장 성공 시 assistant 응답은 사용자가 요청한 정보가 기억에 추가되었음을 명확히 알린다.
- memory 저장 실패는 일반 chat 메시지 저장을 막지 않으며, assistant 응답은 실패 사실을 정확히 안내한다.
- "잊어줘" 같은 자연어 비활성화 명령은 이번 작업에서 처리하지 않는다. 대상 memory ID 또는 현재 memory 문맥이 없어 잘못된 row를 비활성화할 위험이 있으므로, memory context 또는 관리 UI 작업 이후에 다룬다.

## Scope

- AI Chat JSON 계약에 explicit memory 저장 command extraction을 추가한다.
- `AiPromptFactory`에 명시적 memory 명령 판별 규칙을 추가한다.
- `AiChatResult` parsing과 fallback에 memory command 결과를 추가한다.
- `AiChatServiceImpl`이 `UserMemoryService`를 통해 명령을 실행하게 한다.
- Fake LLM harness로 "명령 인식 -> service 호출 -> 응답" 시나리오를 검증한다.

## Do Not Implement

- 일반 대화의 자동 memory 추출
- 자연어 memory 비활성화 또는 삭제 명령
- memory type 분류
- active memory를 ContextBuilder 또는 provider prompt에 포함
- 사용자 memory 목록 UI
- 005에 정의하지 않은 수정/삭제 API

## Invariants

- 현재 사용자 발화가 명시적 memory 명령일 때만 저장을 시도한다.
- LLM이 추측한 정보와 일회성 기록은 memory로 저장하지 않는다.
- chat 계층은 `UserMemoryController` 또는 HTTP API를 호출하지 않는다.
- memory 명령 실패가 USER/ASSISTANT chat message persistence를 막지 않는다.
- 테스트는 실제 provider를 호출하지 않는다.

## Acceptance Criteria

- [x] 명시적 memory 저장 명령이 extraction 결과로 표현된다.
- [x] 일반 정보성 대화는 memory 저장을 유발하지 않는다.
- [x] AI Chat이 authenticated user의 `UserMemoryService`만 호출한다.
- [x] 저장 성공/실패 assistant 응답이 구분된다.
- [x] Fake LLM 기반 routing 테스트가 있다.

## Verification

```bash
cd backend && mvn test
```

## Tests

- 추가: 명시적 저장 명령 routing test
- 추가: 일반 대화가 memory service를 호출하지 않는 test
- 추가: memory 저장 실패가 AI Chat 결과를 실패 안내로 바꾸는 test
- 수정: `AiPromptFactory`, `AiChatServiceImplTest`, `AiChatHarnessTest`
