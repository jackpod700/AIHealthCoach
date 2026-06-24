# 007 User Memory Context And Prompt

## Status

done

## Goal

005에서 저장된 active user memory를 `ContextBuilder`가 수집하고, provider 요청 prompt에 렌더링해 이후 AI Chat 응답이 사용자의 장기 정보를 반영하게 한다.

이 작업은 context를 단순히 `LlmServiceImpl`에 전달하는 방식이 아니라, context 조립·prompt 렌더링·provider 전송의 책임을 분리한다.

## Read First

1. `AGENTS.md`
2. `PROJECT_PROFILE.md`
3. `docs/AI_CHAT/README.md`
4. `backend/docs/ai-chat-context-builder-plan.md`
5. `tasks/003-context-builder.md`
6. `tasks/005-user-memory.md`
7. `backend/src/main/java/com/aihealthcoach/chat/service/ContextBuilderImpl.java`
8. `backend/src/main/java/com/aihealthcoach/chat/service/LlmServiceImpl.java`
9. `backend/src/main/java/com/aihealthcoach/chat/dto/ChatContextDto.java`

## Current Gaps

- `ContextBuilder`는 profile, goal, 당일 기록, 최근 대화를 `UserChatContext`로 조립하지만 active memory는 포함하지 않는다.
- `LlmRequest`는 `UserChatContext`를 보관하지만, `LlmServiceImpl`은 현재 이를 provider 요청에 사용하지 않는다.
- `AiPromptFactory`는 고정 지침과 기준일처럼 매 요청 달라지는 값을 하나의 system prompt 문자열로 만든다.
- `LlmServiceImpl`이 `UserChatContext`를 직접 렌더링하면 provider adapter가 chat, memory, meal 등 도메인 DTO와 결합한다.
- 기존 Fake LLM harness는 raw context 전달만 검증하며, 실제 provider에 보낼 prompt의 memory 렌더링은 검증하지 않는다.

## Target Structure

```text
ContextBuilder
  -> UserChatContext (active memories 포함)

PromptBuilder
  -> stable system prompt + dynamic context prompt + current user message
  -> LlmRequest

LlmServiceImpl
  -> LlmRequest를 Spring AI ChatClient 요청으로 전달
```

### Responsibility Boundaries

| Component | Owns | Must Not Own |
|---|---|---|
| `ContextBuilder` | authenticated user의 읽기 전용 context 조립, source별 조회 limit | prompt 문자열 formatting, provider 호출 |
| `PromptBuilder` | stable 지침과 dynamic context의 prompt 렌더링, memory section 생략 규칙 | DB/service 조회, provider 호출 |
| `AiPromptFactory` | 입력 종류별 stable system instruction | 사용자별 값, 날짜, memory/record text 조립 |
| `AiChatServiceImpl` | context build -> prompt build -> LLM call -> result parsing의 orchestration | context 세부 렌더링, provider-specific request 조립 |
| `LlmServiceImpl` | provider-neutral `LlmRequest`를 Spring AI `ChatClient` 호출로 변환 | `UserChatContext` 또는 도메인 DTO 해석 |

## Detailed Design

### 1. Context Collection

- `UserChatContext`에 active memory 목록을 추가한다.
- `ContextBuilderImpl`은 `UserMemoryService.findActiveMemories(userId, 10)`만 호출한다.
- memory의 최신 수정 순과 최대 10개 제한은 `UserMemoryService`의 기존 계약을 따른다. ContextBuilder의 `10`은 prompt context 정책을 드러내는 호출 limit이고, service의 최대 제한은 방어 규칙으로 유지한다.
- ContextBuilder는 `UserMemoryMapper` 또는 controller를 직접 참조하지 않는다.
- memory가 없으면 empty list를 사용한다. 이는 context 조립 실패가 아니다.

### 2. Prompt Contract

- `LlmRequest`에서 raw `UserChatContext`를 제거한다. provider 경계에는 렌더링 전 domain object를 전달하지 않는다.
- `LlmRequest`는 다음 정보를 표현하도록 변경한다.
  - `stableSystemPrompt`: 사용자마다 변하지 않는 역할, JSON 계약, 안전 규칙
  - `dynamicContextPrompt`: 기준일, profile, goal, 당일 기록, 최근 대화, active memory를 렌더링한 선택적 text
  - `userMessage`: 현재 사용자 발화
  - `images`: 현재 이미지 입력
- `PromptBuilder`가 text/image 입력 종류에 맞는 stable prompt를 선택하고, dynamic context와 현재 입력을 포함한 `LlmRequest`를 만든다.
- `AiPromptFactory`에서는 날짜와 사용자별 데이터를 제거한다. stable prompt는 같은 입력 종류에서 항상 같은 prefix를 유지한다.
- `LlmServiceImpl`은 stable system prompt, optional dynamic context prompt, user message 순서를 유지해 `ChatClient`에 전달한다.

### 3. Memory Rendering Rules

- active memory가 있을 때만 dynamic context에 `<user_memories>` section을 추가한다.
- 각 memory는 content만 렌더링한다. id, active flag, 생성/수정 시각은 provider에 전달하지 않는다.
- memory text는 사용자 제공 정보이며, system instruction 또는 현재 사용자 발화보다 우선하지 않는다고 stable prompt에 명시한다.
- memory 안의 명령형 문장은 실행 지시로 해석하지 않는다.
- memory가 없을 때 `<user_memories>`, `null`, 빈 목록 표시를 렌더링하지 않는다.

### 4. Prompt Cache Direction

- stable system prompt를 가장 앞에 두고, dynamic context와 현재 사용자 발화를 뒤에 둔다.
- 이 구조는 provider의 prefix cache를 활용할 수 있게 하지만, caching 동작 자체를 애플리케이션이 보장하거나 관리하지 않는다.
- 현재 GMS OpenAI proxy가 cache usage를 전달하는지는 이 작업의 검증 대상이 아니다. cache hit 측정과 token budget은 후속 작업으로 남긴다.

### 5. Failure Policy

- memory가 없는 경우는 정상 흐름으로 처리한다.
- memory 조회를 포함한 context source 조회의 예상하지 못한 실패는 기존 `AiChatServiceImpl` fallback 정책을 따른다.
- context/prompt/LLM 실패 뒤에도 `ChatController`는 USER와 ASSISTANT 메시지를 저장한다.

## Scope

- `ContextBuilder`가 active memory를 `UserChatContext`에 포함하도록 수정한다.
- `PromptBuilder`를 추가해 stable prompt, dynamic context, current user message를 provider 요청으로 렌더링한다.
- `LlmRequest`에서 raw context를 제거하고 rendered prompt contract로 변경한다.
- `LlmServiceImpl`이 rendered `LlmRequest`를 실제 provider 요청에 반영하도록 수정한다.
- memory가 없을 때는 빈 section이나 `null` 텍스트를 렌더링하지 않는다.
- memory는 최대 10개로 제한한다.
- Fake LLM harness로 이전에 저장된 memory가 다음 요청의 prompt에 포함되는지 검증한다.

## Do Not Implement

- memory 자동 저장 또는 chat command routing
- type/우선순위/충돌 해결
- daily summary, context token budget, embedding/vector search
- LLM provider 교체 또는 call logging
- provider cache hit 측정 또는 cache retention 설정

## Invariants

- active memory는 authenticated user의 것만 포함한다.
- memory가 없어도 AI Chat은 기존처럼 동작한다.
- memory는 현재 사용자 발화보다 우선하지 않는다.
- provider adapter는 `UserChatContext` 또는 memory domain DTO를 직접 참조하지 않는다.
- stable prompt에는 사용자별 데이터나 기준일을 넣지 않는다.
- 실제 provider 호출 없이 prompt rendering을 테스트할 수 있다.

## Acceptance Criteria

- [x] active memory가 `UserChatContext`에 포함된다.
- [x] provider prompt에 active memory가 렌더링된다.
- [x] memory가 없는 prompt는 불필요한 빈 section을 포함하지 않는다.
- [x] 최대 10개 memory 제한이 지켜진다.
- [x] `LlmServiceImpl`이 raw `UserChatContext`에 의존하지 않는다.
- [x] stable prompt와 dynamic context prompt의 책임이 분리된다.
- [x] Fake LLM harness가 다음 요청의 memory 반영을 검증한다.

## Verification

```bash
cd backend && mvn test
```

## Tests

- 추가: ContextBuilder active memory 조립 test
- 추가: PromptBuilder memory 렌더링 test
- 추가: memory가 없는 dynamic context prompt test
- 추가: memory가 instruction처럼 보이는 text를 포함해도 data section으로 렌더링되는 test
- 추가: Fake LLM memory prompt 시나리오 test
- 수정: `ContextBuilderImplTest`, `AiChatHarnessTest`, `AiChatServiceImplTest`, `LlmServiceImpl` 관련 test
