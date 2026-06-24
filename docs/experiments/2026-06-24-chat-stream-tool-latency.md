# Chat Stream Tool Latency

## 목적

AI Chat 스트리밍에서 assistant 답변은 화면에 빨리 나오지만, 기록 후보(tool result) 카드가 늦게 표시되는 원인을 확인하고 tool 경로의 지연을 줄인다.

## 측정 대상

- Backend flow: `ChatStreamingOrchestrator`
- Frontend flow: `chatStore.sendMessage`
- Provider path: `LlmService.generate()` -> `AiChatClientGateway.callTextChat()`
- Model config: `spring.ai.openai.chat.options.model=gpt-4o-mini`
- Provider base URL: GMS OpenAI 호환 gateway

## 측정 방법

서버 로그에 스트리밍 단계별 타이밍을 추가했다.

| Metric | 의미 |
|---|---|
| `first_delta_ms` | 요청 시작 후 첫 assistant delta까지 걸린 시간 |
| `assistant_stream_ms` | 첫 delta 이후 assistant stream 완료까지 걸린 시간 |
| `assistant_save_ms` | assistant 메시지 DB 저장 시간 |
| `tool_total_ms` | tool task 전체 시간 |
| `tool_join_wait_ms` | assistant 저장 후 tool 완료를 기다린 시간 |
| `tool_prompt_built_ms` | tool prompt 생성 시간 |
| `tool_llm_generate_ms` | tool LLM non-streaming 호출 시간 |
| `tool_response_chars` | tool LLM 응답 문자 수 |
| `tool_parse_ms` | JSON parse 및 normalize 시간 |
| `tool_proposal_ms` | meal/exercise/weight proposal 변환 및 memory save 시간 |

프론트에는 dev console 로그를 추가했다.

| Event | 의미 |
|---|---|
| `assistant_done` | backend가 assistant 저장 완료 metadata를 보낸 시점 |
| `assistant_done_applied` | frontend가 assistant pending을 종료한 시점 |
| `tool_result` | frontend가 tool result SSE를 받은 시점 |
| `proposal_applied` | frontend가 proposal state를 반영한 시점 |

## 변경 전 관측

초기 로그에서 tool result 지연은 frontend flush나 렌더링이 아니라 backend tool task 대기였다.

| Sample | `tool_total_ms` | `tool_join_wait_ms` | 해석 |
|---|---:|---:|---|
| A | 3912 | 2354 | assistant 저장 후 tool result를 2.3초 추가 대기 |
| B | 8865 | 6271 | assistant 저장 후 tool result를 6.2초 추가 대기 |

tool 내부 타이밍을 쪼갠 뒤에는 지연 대부분이 LLM provider 호출임을 확인했다.

| Sample | `tool_prompt_built_ms` | `tool_llm_generate_ms` | `tool_response_chars` | `tool_parse_ms` | `tool_proposal_ms` | `tool_total_ms` |
|---|---:|---:|---:|---:|---:|---:|
| C | 0 | 6086 | 643 | 20 | 216 | 6323 |

## 적용한 변경

### 1. Assistant 완료 UI 분리

기존 frontend는 `assistant_done`을 받아도 assistant pending을 끄지 않고, SSE stream 종료와 reveal flush 이후에 assistant 완료와 proposal 반영을 같이 처리했다.

변경 후에는 `assistant_done` 수신 시 즉시 assistant pending을 종료한다. tool result가 늦어도 assistant 답변 완료 상태는 먼저 표시된다.

### 2. Tool prompt 압축

기존 tool prompt는 `commonPrompt() + textExtractionRules()`를 사용했다. 이 경로에는 다음 내용이 포함되어 있었다.

- `assistantMessage` 생성 요구
- health coaching 성격의 공통 지시
- 전체 JSON schema
- 일반 대화에서도 JSON 유지 지시

변경 후 tool prompt는 스트리밍 tool 전용 compact prompt로 분리했다.

- `assistantMessage` 출력 제거
- health coaching 문구 제거
- current user message 기반 기록 후보 추출만 지시
- false intent section은 false/null/empty 최소 shape 유지

### 3. 명백한 일반 대화 tool LLM 스킵

추천, 영양 질문, 인사처럼 기록 후보가 명백히 필요 없는 발화는 tool LLM 호출을 생략한다.

스킵된 경우 `chat_stream_tool_timing`은 다음 형태로 남는다.

```text
status=SKIPPED reason=OBVIOUS_GENERAL_CHAT tool_llm_generate_ms=-1
```

기록 단서가 있는 발화는 스킵하지 않는다.

| 발화 예시 | 처리 |
|---|---|
| `점심 뭐 먹을까 추천해줘` | tool LLM skip |
| `점심 라면 먹었어` | tool LLM 호출 |
| `오늘 68.4kg이야` | tool LLM 호출 |
| `유제품 피하는 걸 기억해줘` | tool LLM 호출 |

## 변경 후 관측

수동 로그 샘플 기준으로 tool response 크기와 tool LLM 시간이 줄었다.

| Sample | `tool_llm_generate_ms` | `tool_response_chars` | `tool_parse_ms` | `tool_proposal_ms` | `tool_total_ms` | `tool_join_wait_ms` |
|---|---:|---:|---:|---:|---:|---:|
| Before C | 6086 | 643 | 20 | 216 | 6323 | - |
| After D | 3782 | 423 | 22 | 14 | 3819 | 949 |
| After E | 2552 | 416 | 0 | 0 | 2552 | 32 |

assistant 전체 흐름과 비교하면 두 번째 after sample에서는 tool이 assistant 완료 시점과 거의 맞물렸다.

```text
first_delta_ms=780 assistant_stream_ms=1733 assistant_save_ms=17
tool_total_ms=2564 tool_join_wait_ms=32 total_ms=2564
```

## 해석

- 지연 원인은 frontend flush나 JSON parse가 아니라 `tool_llm_generate_ms`, 즉 tool JSON 생성을 위한 non-streaming LLM 호출이었다.
- prompt 압축 후 `tool_response_chars`는 약 640자대에서 420자대로 줄었다.
- 수동 샘플 기준 `tool_total_ms`는 약 6.3초에서 2.5-3.8초로 줄었다.
- 일반 대화 스킵은 기록 후보가 필요 없는 발화에서 tool 비용과 tail latency를 제거하는 가장 큰 개선 여지가 있다.

## 한계

- 이 기록은 동일 부하와 동일 provider 상태에서 반복 측정한 benchmark가 아니라 개발 중 수동 로그 샘플이다.
- GMS/OpenAI gateway의 순간 지연이 섞일 수 있다.
- 한국어 keyword 기반 skip rule은 보수적으로 설계했지만, 추천과 기록이 섞인 발화에서는 tool LLM을 호출할 수 있다.

## 다음 후보

1. `status=SKIPPED`가 실제 추천/일반 질문에서 충분히 발생하는지 로그로 확인한다.
2. tool 전용 model/options를 분리해 `max_tokens` 또는 더 빠른 모델을 적용한다.
3. 더 큰 변경이 가능하면 tool 전용 compact DTO/schema를 도입해 response key 자체를 줄인다.
4. 충분한 샘플을 모아 p50/p95 기준으로 재측정한다.

## 검증

```bash
cd backend
mvn -Dmaven.repo.local=/tmp/m2-aihealthcoach -Dtest=ChatStreamingOrchestratorTest,PromptBuilderImplTest test
```

결과: 통과.
